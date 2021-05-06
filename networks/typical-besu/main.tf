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
      port = {
        http    = 8545,
        ws      = 8546,
        graphql = 8547,
        p2p     = 30303
      }
    }
    host = {
      port = {
        http_start    = 23000,
        ws_start      = 23100,
        graphql_start = 23200
      }
    }
  }
  tessera = {
    container = {
      image = var.tessera_docker_image
      port = {
        thirdparty = 9080,
        p2p        = 9000,
        q2t        = 9081
      }
    }
    host = {
      port = {
        thirdparty_start = 24000,
        q2t_start        = 24100
      }
    }
  }
  ethsigner = {
    container = {
      image = var.ethsigner_docker_image
      port  = 8545
    }
    host = {
      port_start = 22000
    }
  }
}

module "network" {
  source = "../_modules/ignite-besu"

  consensus            = module.helper.consensus
  network_name         = var.network_name
  besu_networking      = module.helper.besu_networking
  tm_networking        = module.helper.tm_networking
  ethsigner_networking = module.helper.ethsigner_networking
  output_dir           = var.output_dir
}

module "docker" {
  source = "../_modules/docker-besu"

  chainId              = module.network.chainId
  besu_networking      = module.helper.besu_networking
  ethsigner_networking = module.helper.ethsigner_networking
  tm_networking        = module.helper.tm_networking
  network_cidr         = module.helper.network_cidr

  network_name           = module.network.network_name
  network_id             = module.network.network_id
  node_keys_hex          = module.network.node_keys_hex
  besu_datadirs          = var.remote_docker_config == null ? module.network.besu_dirs : split(",", join("", null_resource.scp[*].triggers.data_dirs))
  tessera_datadirs       = var.remote_docker_config == null ? module.network.tm_dirs : split(",", join("", null_resource.scp[*].triggers.tm_dirs))
  ethsigner_datadirs     = var.remote_docker_config == null ? module.network.ethsigner_dirs : split(",", join("", null_resource.scp[*].triggers.ethsigner_dirs))
  keystore_files         = module.network.keystore_files
  keystore_password_file = module.network.keystore_password_file

  tessera_app_container_path = var.tessera_app_container_path
  accounts_count             = module.network.accounts_count
}
