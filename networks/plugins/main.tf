locals {
  standard_apis = "admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,${var.consensus}"
  plugin_apis   = [for k, v in var.plugins : "plugin@${k}" if v.expose_api]
  apis          = "${local.standard_apis},${join(",", local.plugin_apis)}"

  node_indices = range(var.number_of_nodes)

  providers = { for k, v in var.plugins : k => { name = v.name, version = v.version, config = format("file://%s/plugins/%s-config.json", module.docker.container_geth_datadir, k) } }
}

provider "docker" {
  dynamic "registry_auth" {
    for_each = var.docker_registry
    content {
      address  = registry_auth.value["name"]
      username = registry_auth.value["username"]
      password = registry_auth.value["password"]
    }
  }
}

resource "local_file" "plugin-settings" {
  count    = var.number_of_nodes
  filename = format("%s/plugin-settings.json", module.network.data_dirs[count.index])
  content  = <<JSON
{
	"providers": ${jsonencode(local.providers)}
}
JSON
}

resource "local_file" "docker" {
  filename = format("%s/application-docker.yml", module.network.generated_dir)
  content  = <<YML
quorum:
  consensus: ${var.consensus}
  docker-infrastructure:
    enabled: true
    nodes:
%{for idx in local.node_indices~}
      Node${idx + 1}:
        quorum-container-id: ${element(module.docker.quorum_containers, idx)}
        tessera-container-id: ${element(module.docker.tessera_containers, idx)}
%{endfor~}
YML
}