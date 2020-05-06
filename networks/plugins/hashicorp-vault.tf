locals {
    vault_server_container_name = format("%s-vault-server", var.network_name)
    //TODO(cjh) this is used as a workaround for the SAN's defined in the certificates used by the vault server. New certificates should be generated for the {network-name}-vault-server SAN
    cert_san_workaround_hostname = "node1"
    vault_server_port  = { internal = 8222, external = 8222 }
    vault_server_token = "s.K0tuWLNVlyBCPLKCm39QOJvc"
    vault_server_unseal_key = "10gYKD89Wa5T3lomqComqrBYEwAiGFSKgpgehNY9LJk="

    host_vault_storage_dir = abspath("vault-server/vault-storage")
    container_vault_storage_dir = "/vault-storage"

    host_certs_dir = abspath("vault-server/CertsWithQuorumRootCAandIntCA")
    container_certs_dir = "/certs"
    container_server_cert = "${local.container_certs_dir}/localhost-with-san-chain.pem"
    container_server_key  = "${local.container_certs_dir}/localhost-with-san.key"
    container_client_cert = "${local.container_certs_dir}/quorum-client-chain.pem"
    container_client_key = "${local.container_certs_dir}/quorum-client.key"
    container_ca_cert = "${local.container_certs_dir}/caRoot.pem"

    network_config = tolist(data.docker_network.quorum.ipam_config)[0]
}

data "docker_network" "quorum" {
    name = module.docker.docker_network_name
}

data "docker_registry_image" "vault" {
    // TODO(cjh) not using 1.4.x due to TLS issue with spring-cloud-vault https://github.com/hashicorp/vault/issues/8750. Revisit
    name = "vault:1.3.5"
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
      name = data.docker_network.quorum.name
      ipv4_address = cidrhost(lookup(local.network_config, "subnet"), 200)
      aliases = [local.cert_san_workaround_hostname]
    }
    ports {
      internal = local.vault_server_port.internal
      external = local.vault_server_port.external
    }
    mounts {
        type = "bind"
        source = local.host_vault_storage_dir
        target = local.container_vault_storage_dir
    }
    mounts {
        type = "bind"
        source = local.host_certs_dir
        target = local.container_certs_dir
    }
    upload {
        file    = "/vault/config/quorum-vault.hcl"
        content = <<EOF
storage "file" {
	path = "${local.container_vault_storage_dir}"
}

listener "tcp" {
        address = "0.0.0.0:${local.vault_server_port.internal}"
        tls_disable = 0
        tls_min_version = "tls12"
        tls_cert_file = "${local.container_server_cert}"
        tls_key_file = "${local.container_server_key}"
        tls_require_and_verify_client_cert = "true"
        tls_client_ca_file = "${local.container_ca_cert}"
}
EOF
    }
    restart = "unless-stopped"
    capabilities {
        add = ["IPC_LOCK"]
    }
    env = [
        "VAULT_ADDR=https://127.0.0.1:${local.vault_server_port.internal}",
        "VAULT_CACERT=${local.container_ca_cert}",
        "VAULT_CLIENT_CERT=${local.container_client_cert}",
        "VAULT_CLIENT_KEY=${local.container_client_key}"
    ]
    command = ["server"]
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
