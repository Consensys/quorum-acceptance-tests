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
  more_args = join(" ", [
    # since 1.9.7 upgrade, --allow-insecure-unlock is needed§
    "--allow-insecure-unlock",
    "--revertreason"
  ])
}

module "helper" {
  source = "../_modules/docker-helper"

  consensus       = var.consensus
  number_of_nodes = local.number_of_nodes
  geth = {
    container = {
      image = var.quorum_docker_image
      port = {
        raft = 50400,
        p2p  = 21000,
        http = 8545,
        ws   = -1
      }
      graphql = true
    }
    host = {
      port = {
        http_start = 22000,
        ws_start   = -1
      }
    }
  }
  tessera = {
    container = {
      image = var.tessera_docker_image
      port = {
        thirdparty = 9080,
        p2p        = 9000,
        q2t        = 9081,
        q2t        = 9081
      }
    }
    host = {
      port = {
        thirdparty_start = 9080,
        q2t_start        = 49081
      }
    }
  }
}

module "network" {
  source = "../_modules/ignite"

  consensus            = module.helper.consensus
  privacy_enhancements = var.privacy_enhancements
  privacy_precompile = var.privacy_precompile
  network_name         = var.network_name
  geth_networking      = module.helper.geth_networking
  tm_networking        = module.helper.tm_networking
  output_dir           = var.output_dir
  qbftBlock            = var.qbftBlock

  qbft_empty_block_period = var.qbft_empty_block_period

  override_tm_named_key_allocation  = var.override_tm_named_key_allocation
  override_named_account_allocation = var.override_named_account_allocation
  override_vnodes                   = var.override_vnodes

  additional_tessera_config = var.additional_tessera_config
  additional_genesis_config = var.additional_genesis_config
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
  privacy_marker_transactions = var.privacy_marker_transactions

  additional_geth_args             = { for idx in local.node_indices : idx => local.more_args }
  additional_geth_container_vol    = var.additional_quorum_container_vol
  additional_tessera_container_vol = var.additional_tessera_container_vol
  tessera_app_container_path       = var.tessera_app_container_path
  accounts_count                   = module.network.accounts_count
}
