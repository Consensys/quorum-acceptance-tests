locals {
    host_certs_dir = abspath("vault-server/dev-certs")

    quorum_container_ca_cert = "certs/ca-root.cert.pem"
    quorum_container_client_cert = "certs/client-ca-chain.cert.pem"
    quorum_container_client_key = "certs/client.key.pem"

    vault_client_truststore = "${local.host_certs_dir}/truststore.jks"
    vault_client_truststore_pwd = "testtest"
    vault_client_keystore = "${local.host_certs_dir}/client.jks"
    vault_client_keystore_pwd = "testtest"

    plugin_token_envvar_name = "HASHICORP_TOKEN"

    hashicorp_vault_plugin_spring_profile = "hashicorp-vault-plugin"

    host_plugin_acct_dirs = [for d in module.network.data_dirs : "${d}/plugin-accts"]
}

# this resource creates additional Spring Application YML file
# which is merged with default one when running test
resource "local_file" "hashicorp-vault-test-properties" {
    filename = format("%s/application-%s.yml", module.network.generated_dir, local.hashicorp_vault_plugin_spring_profile)
    content = <<YML
quorum:
    hashicorp-vault-server:
        url: https://localhost:${local.vault_server_port.external}
        tls-trust-store-path: ${local.vault_client_truststore}
        tls-trust-store-password: ${local.vault_client_truststore_pwd}
        tls-key-store-path: ${local.vault_client_keystore}
        tls-key-store-password: ${local.vault_client_keystore_pwd}
        auth-token: ${local.vault_server_token}
        node-acct-dirs:
%{for idx in local.node_indices~}
            Node${idx + 1}:
                plugin-acct-dir: ${local.host_plugin_acct_dirs[idx]}
%{endfor~}
YML
}

// each node will be configured with the same vault server as this is all that is currently required for the tests
resource "local_file" "hashicorp-vault-account-plugin-config" {
    count    = var.number_of_nodes
    filename = format("%s/plugins/account-config.json", module.network.data_dirs[count.index])
    content  = <<JSON
{
    "vault": "https://${local.cert_san_workaround_hostname}:${local.vault_server_port.external}",
    "kvEngineName": "kv",
    "accountDirectory": "file:///data/plugin-accts",
    "authentication": {
        "token": "env://${local.plugin_token_envvar_name}"
    },
    "tls": {
        "caCert": "${format("file:///data/qdata/%s", local.quorum_container_ca_cert)}",
        "clientCert": "${format("file:///data/qdata/%s", local.quorum_container_client_cert)}",
        "clientKey": "${format("file:///data/qdata/%s", local.quorum_container_client_key)}"
    }
}
JSON
}

//TODO(cjh) configurable mounts/volumes on the quorum/tessera containers
data "local_file" "host-ca-cert" {
    filename = "${local.host_certs_dir}/ca-root.cert.pem"
}
resource "local_file" "container-ca-cert" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.quorum_container_ca_cert)
    content  = data.local_file.host-ca-cert.content
}

data "local_file" "host-client-cert" {
    filename = "${local.host_certs_dir}/client-ca-chain.cert.pem"
}
resource "local_file" "container-client-cert" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.quorum_container_client_cert)
    content  = data.local_file.host-client-cert.content
}

data "local_file" "host-client-key" {
    filename = "${local.host_certs_dir}/client.key.pem"
}
resource "local_file" "container-client-key" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.quorum_container_client_key)
    content  = data.local_file.host-client-key.content
}
