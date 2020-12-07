locals {
  standard_apis = "admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,quorumExtension,${var.consensus}"
  plugin_apis   = [for k, v in var.plugins : "plugin@${k}" if v.expose_api]
  apis          = "${local.standard_apis},${join(",", local.plugin_apis)}"
  more_args = join(" ", [
    "--allow-insecure-unlock" # since 1.9.7 upgrade
  ])

  node_indices = range(var.number_of_nodes)

  providers = { for k, v in var.plugins : k => { name = v.name, version = v.version, config = format("file://%s/plugins/%s-config.json", module.docker.container_geth_datadir, k) } }

  with_hashicorp_plugin = contains(values(var.plugins)[*].name, "quorum-account-plugin-hashicorp-vault")
}

provider "docker" {
  host = var.remote_docker_config == null ? null : var.remote_docker_config.docker_host

  dynamic "registry_auth" {
    for_each = var.docker_registry
    content {
      address  = registry_auth.value["name"]
      username = registry_auth.value["username"]
      password = registry_auth.value["password"]
    }
  }
}

module "helper" {
  source = "../_modules/docker-helper"

  consensus       = var.consensus
  number_of_nodes = var.number_of_nodes
  geth = {
    container = {
      image = var.quorum_docker_image
      port  = { raft = 50400, p2p = 21000, http = 8545, ws = -1, graphql = 8547 }
    }
    host = {
      port = { http_start = 22000, ws_start = -1, graphql_start = 8001 }
    }
  }
  tessera = {
    container = {
      image = var.tessera_docker_image
      port  = { thirdparty = 9080, p2p = 9000 }
    }
    host = {
      port = { thirdparty_start = 9080 }
    }
  }
}

module "network" {
  source = "../_modules/ignite"

  concensus            = module.helper.consensus
  privacy_enhancements = var.privacy_enhancements
  network_name         = var.network_name
  geth_networking      = module.helper.geth_networking
  tm_networking        = module.helper.tm_networking
  output_dir           = var.output_dir
}

module "docker" {
  source = "../_modules/docker"

  consensus       = module.helper.consensus
  geth_networking = module.helper.geth_networking
  tm_networking   = module.helper.tm_networking
  network_cidr    = module.helper.network_cidr
  ethstats_ip     = module.helper.ethstat_ip
  ethstats_secret = module.helper.ethstats_secret

  network_name       = module.network.network_name
  network_id         = module.network.network_id
  node_keys_hex      = module.network.node_keys_hex
  password_file_name = module.network.password_file_name
  geth_datadirs      = var.remote_docker_config == null ? module.network.data_dirs : split(",", join("", null_resource.scp[*].triggers.data_dirs))
  tessera_datadirs   = var.remote_docker_config == null ? module.network.tm_dirs : split(",", join("", null_resource.scp[*].triggers.tm_dirs))

  # provide additional geth args
  additional_geth_args = format("--rpcapi %s --plugins file://%s/plugin-settings.json %s", local.apis, "/data/qdata", local.more_args)
  additional_geth_env = {
    (local.plugin_token_envvar_name) = local.vault_server_token
  }

  host_plugin_account_dirs = local.host_plugin_acct_dirs
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
