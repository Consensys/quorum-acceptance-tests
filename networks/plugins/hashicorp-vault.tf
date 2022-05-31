locals {
  vault_server_container_name = format("%s-vault-server", var.network_name)
  //TODO(cjh) this is used as a workaround for the SAN's defined in the certificates used by the vault server. New certificates should be generated for the {network-name}-vault-server SAN
  cert_san_workaround_hostname = "node1"
  vault_server_port            = { internal = 8222, external = 8222 }
  vault_server_token           = "s.TZG2LuIkjcT9AYRNZfHrmuQn"
  vault_server_unseal_key      = "Xg/nHOs0/uuckKjcszobas4aVNjFxyRP4GtsmlmnV4U="

  container_entrypoint = "quorum-docker-entrypoint.sh"

  host_certs_zip              = abspath("vault-server/certs.zip")
  vault_container_certs_zip   = "/certs.zip"
  vault_container_certs_dir   = "/certs"
  vault_container_server_cert = "${local.vault_container_certs_dir}/server-localhost-with-san-ca-chain.cert.pem"
  vault_container_server_key  = "${local.vault_container_certs_dir}/server-localhost-with-san.key.pem"
  vault_container_client_cert = "${local.vault_container_certs_dir}/client-ca-chain.cert.pem"
  vault_container_client_key  = "${local.vault_container_certs_dir}/client.key.pem"
  vault_container_ca_cert     = "${local.vault_container_certs_dir}/ca-root.cert.pem"

  host_vault_storage_zip            = abspath("vault-server/vault.zip")
  vault_container_vault_storage_zip = "/vault.zip"
  vault_container_vault_storage_dir = "/vault-storage"
}

data "docker_registry_image" "vault" {
  // TODO(cjh) not using 1.4.x due to TLS issue with spring-cloud-vault https://github.com/hashicorp/vault/issues/8750. Revisit
  name = "vault:latest"
}

resource "docker_image" "vault" {
  name          = data.docker_registry_image.vault.name
  pull_triggers = [data.docker_registry_image.vault.sha256_digest]
}

resource "docker_container" "vault_server" {
  image    = docker_image.vault.latest
  name     = local.vault_server_container_name
  hostname = local.vault_server_container_name
  networks_advanced {
    name         = module.docker.docker_network_name
    ipv4_address = cidrhost(lookup(local.network_config, "subnet"), 201)
    aliases      = [local.cert_san_workaround_hostname]
  }
  ports {
    internal = local.vault_server_port.internal
    external = local.vault_server_port.external
  }
  upload {
    source = local.host_certs_zip
    file   = local.vault_container_certs_zip
  }
  upload {
    source = local.host_vault_storage_zip
    file   = local.vault_container_vault_storage_zip
  }
  upload {
    file    = "/vault/config/quorum-vault.hcl"
    content = <<EOF
storage "file" {
	path = "${local.vault_container_vault_storage_dir}"
}

listener "tcp" {
        address = "0.0.0.0:${local.vault_server_port.internal}"
        tls_disable = 0
        tls_min_version = "tls12"
        tls_cert_file = "${local.vault_container_server_cert}"
        tls_key_file = "${local.vault_container_server_key}"
        tls_require_and_verify_client_cert = "true"
        tls_client_ca_file = "${local.vault_container_ca_cert}"
}
EOF
  }
  upload {
    executable = true
    file       = "/usr/local/bin/${local.container_entrypoint}"
    content    = <<EOF
#!/usr/bin/dumb-init /bin/sh
set -e
mkdir ${local.vault_container_certs_dir} ${local.vault_container_vault_storage_dir}
unzip ${local.vault_container_certs_zip} -d ${local.vault_container_certs_dir}
unzip ${local.vault_container_vault_storage_zip} -d ${local.vault_container_vault_storage_dir}
chmod -R 777 ${local.vault_container_vault_storage_dir}
docker-entrypoint.sh "$@"
EOF
  }
  restart = "unless-stopped"
  capabilities {
    add = ["IPC_LOCK"]
  }
  env = [
    "VAULT_ADDR=https://127.0.0.1:${local.vault_server_port.internal}",
    "VAULT_CACERT=${local.vault_container_ca_cert}",
    "VAULT_CLIENT_CERT=${local.vault_container_client_cert}",
    "VAULT_CLIENT_KEY=${local.vault_container_client_key}"
  ]
  entrypoint = [local.container_entrypoint]
  command    = ["server"]
  healthcheck {
    interval = "5s"
    test = [
      "CMD",
      "/bin/sh",
      "-c",
      <<RUN
SEALED=$(vault operator unseal ${local.vault_server_unseal_key} | grep Sealed | sed -e 's/Sealed//g' -e 's/ //g')
if [ $SEALED = "false" ]
then
  exit 0
else
  exit 1
fi
RUN
    ]
  }
}
