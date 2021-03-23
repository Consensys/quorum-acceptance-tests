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

locals {
  number_of_nodes = var.number_of_nodes
  node_indices    = range(local.number_of_nodes)
}

module "helper" {
  source = "../_modules/docker-helper"

  consensus       = var.consensus
  number_of_nodes = local.number_of_nodes
  besu = {
    container = {
      image = var.besu_docker_image
      port  = { http = 8545 }
    }
    host = {
      port = { http_start = 22000 }
    }
  }
  tessera = {
    container = {
      image = var.tessera_docker_image
      port  = { thirdparty = 9080, p2p = 9000, q2t = 9081 }
    }
    host = {
      port = { thirdparty_start = 9080, q2t_start = 9081 }
    }
  }
  ethsigner = {
    container = {
      image = var.ethsigner_docker_image
      port  = { http = 8545 }
    }
    host = {
      port = { http_start = 23000 }
    }
  }
}

module "network" {
  source = "../_modules/ignite-besu"

  concensus            = module.helper.consensus
  network_name         = var.network_name
  besu_networking      = module.helper.besu_networking
  tm_networking        = module.helper.tm_networking
  ethsigner_networking = module.helper.ethsigner_networking
  output_dir           = var.output_dir
}

//module "docker" {
//  source = "../_modules/docker"
//
//  consensus       = module.helper.consensus
//  geth_networking = module.helper.geth_networking
//  tm_networking   = module.helper.tm_networking
//  network_cidr    = module.helper.network_cidr
//  ethstats_ip     = module.helper.ethstat_ip
//  ethstats_secret = module.helper.ethstats_secret
//
//  network_name     = module.network.network_name
//  network_id       = module.network.network_id
//  node_keys_hex    = module.network.node_keys_hex
//  geth_datadirs    = var.remote_docker_config == null ? module.network.data_dirs : split(",", join("", null_resource.scp[*].triggers.data_dirs))
//  tessera_datadirs = var.remote_docker_config == null ? module.network.tm_dirs : split(",", join("", null_resource.scp[*].triggers.tm_dirs))
//
//  additional_geth_container_vol    = var.additional_quorum_container_vol
//  additional_tessera_container_vol = var.additional_tessera_container_vol
//  tessera_app_container_path       = var.tessera_app_container_path
//  accounts_count                   = module.network.accounts_count
//}
