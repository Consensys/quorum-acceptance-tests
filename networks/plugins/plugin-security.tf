locals {
  oauth2_server            = format("%s-oauth2-server", var.network_name)
  oauth2_server_serve_port = { internal = 4444, external = 4444 } # for client to connect and authenticate
  oauth2_server_admin_port = { internal = 4445, external = 4445 } # for admin
  include_security         = lookup(var.plugins, "security", null) != null

  support_security = { for idx in local.node_indices : format("Node%d", idx + 1) => lookup(lookup(var.override_plugins, idx, var.plugins), "security", null) != null }

  network_config = element(tolist(module.docker.docker_network_ipam_config), 0)

  application_yml = yamldecode(file(module.network.application_yml_file))
}

# this resource creates additional Spring Application YML file
# which would be merged with default one when running test
resource "local_file" "additional" {
  count    = local.include_security ? 1 : 0
  filename = format("%s/application-security.yml", module.network.generated_dir)
  content  = <<YML
quorum:
  oauth2-server:
    client-endpoint: https://localhost:${local.oauth2_server_serve_port.external}
    admin-endpoint: https://localhost:${local.oauth2_server_admin_port.external}
  nodes:
%{for id, n in local.application_yml.quorum.nodes~}
%{if lookup(local.support_security, id, false)~}
    ${id}:
      url: ${replace(replace(lookup(n, "url"), "http://", "https://"), "ws://", "wss://")}
      graphql-url: ${replace(replace(lookup(n, "graphql-url"), "http://", "https://"), "ws://", "wss://")}
%{endif~}
%{endfor~}
YML
}

resource "local_file" "security-config" {
  count = local.include_security ? var.number_of_nodes : 0
  # file name convention is <plugin_interface_name>-config.json
  # which is being used while writing plugin-settings.json
  filename = format("%s/plugins/security-config.json", module.network.data_dirs[count.index])
  content  = <<JSON
{
  "tls": {
    "auto": true,
    "certFile": "/tmp/cert.pem",
    "keyFile": "/tmp/key.pem"
  },
  "tokenValidation": {
    "issuers": [
      "https://goquorum.com/oauth/"
    ],
    "jws": {
      "endpoint": "https://${local.oauth2_server}:${local.oauth2_server_serve_port.internal}/.well-known/jwks.json",
      "tlsConnection": {
        "insecureSkipVerify": true
      }
    },
    "jwt": {
      "authorizationField": "scp",
      "preferIntrospection": false
    }
  }
}
JSON
}
