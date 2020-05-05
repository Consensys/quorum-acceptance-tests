locals {
    vault_client_truststore = "${local.host_certs_dir}/truststore.jks"
    vault_client_truststore_pwd = "testtest"
    vault_client_keystore = "${local.host_certs_dir}/quorum-client-keystore.jks"
    vault_client_keystore_pwd = "testtest"

    plugin_token_envvar_name = "HASHICORP_TOKEN"

    hashicorp_vault_plugin_spring_profile = "hashicorp-vault-plugin"

    host_plugin_acct_dirs = [for d in module.network.data_dirs : "${d}/plugin-accts"]
    host_keystore_acct_dirs = [for d in module.network.data_dirs : "${d}/keystore"]
}

// make sure the plugin account config directories exist for each node
resource "local_file" "plugin_acct_dir" {
    count = length(local.host_plugin_acct_dirs)
    filename = "${local.host_plugin_acct_dirs[count.index]}/tmp"
    content = "{}"
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
                keystore-acct-dir: ${local.host_keystore_acct_dirs[idx]}
%{endfor~}
YML
}

resource "local_file" "hashicorp-vault-account-plugin-config" {
    count    = var.number_of_nodes
    filename = format("%s/plugins/account-config.json", module.network.data_dirs[count.index])
    content  = <<JSON
{
    "vault": "https://${local.cert_san_workaround_hostname}:${local.vault_server_port.external}",
    "accountDirectory": "file:///data/plugin-accts",
    "authentication": {
        "token": "env://${local.plugin_token_envvar_name}"
    },
    "tls": {
        "caCert": "${format("file:///data/qdata/%s", local.container_ca_cert)}",
        "clientCert": "${format("file:///data/qdata/%s", local.container_client_cert)}",
        "clientKey": "${format("file:///data/qdata/%s", local.container_client_key)}"
    }
}
JSON
}

//TODO configurable mounts/volumes on the quorum/tessera containers
data "local_file" "local-ca-cert" {
    filename = "${local.host_certs_dir}/caRoot.pem"
}
resource "local_file" "node-ca-cert" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.container_ca_cert)
    content  = data.local_file.local-ca-cert.content
}

data "local_file" "local-client-cert" {
    filename = "${local.host_certs_dir}/quorum-client-chain.pem"
}
resource "local_file" "node-client-cert" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.container_client_cert)
    content  = data.local_file.local-client-cert.content
}

data "local_file" "local-client-key" {
    filename = "${local.host_certs_dir}/quorum-client.key"
}
resource "local_file" "node-client-key" {
    count    = var.number_of_nodes
    filename = format("%s/%s", module.network.data_dirs[count.index], local.container_client_key)
    content  = data.local_file.local-client-key.content
}
