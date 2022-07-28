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
  generated_dir          = var.output_dir
  network_name           = var.network_name
  ethsigner_dir_prefix   = "ethsigner-"
  quorum_node_dir_prefix = "quorum-"
  keystore_folder        = "keystore"
  keystore_password_file = "keystore_password"
  keystore_password      = "besito1"

  number_of_quorum_nodes  = var.number_of_quorum_nodes
  number_of_besu_nodes    = var.number_of_besu_nodes
  number_of_tessera_nodes = var.number_of_quorum_nodes + var.number_of_besu_nodes
  hybrid_network          = var.hybrid-network
  quorum_node_indices     = range(local.number_of_quorum_nodes)
  besu_node_indices       = range(local.number_of_besu_nodes)
  tessera_node_indices    = range(local.number_of_tessera_nodes)
  more_args = join(" ", [
    "--allow-insecure-unlock" # since 1.9.7 upgrade
  ])
  istanbul_validators = merge(
    { for id in local.tessera_node_indices : id => "true" }, // default to true for all
    { for id in var.exclude_initial_nodes : id => "false" }
  )
  besu_node_initial_paticipants = { for id in local.besu_node_indices : id => "true" }
  quorum_initial_paticipants    = { for id in local.quorum_node_indices : id => "true" }

  ethsigner_dirs = [for idx in local.besu_node_indices : format("%s/%s%s", quorum_bootstrap_network.this.network_dir_abs, local.ethsigner_dir_prefix, idx)]

  // by default we allocate one named account per node
  // this can be overrriden by the variable
  named_accounts_alloc = { for id in local.tessera_node_indices : id => [
  "Default"] }

  // by default we allocate one named key per TM: K0, K1 ... Kn
  // this can be overrriden by the variable
  tm_named_keys_alloc = merge(
    { for id in local.tessera_node_indices : id => [format("UnnamedKey%d", id)] },
    var.override_tm_named_key_allocation
  )

  tmkeys_generated_dir = "${local.generated_dir}/${local.network_name}/tmkeys"
  tm_named_keys_all    = flatten(values(local.tm_named_keys_alloc))

  keystore_files = [for idx in local.besu_node_indices : format("%s/%s", local.keystore_folder, regex("UTC.+$", quorum_bootstrap_keystore.besu-accountkeys-generator[idx].account[0].account_url))]

  geth_networking = [for idx in local.quorum_node_indices :
    {
      image = var.geth.container.image
      port = {
        http = { internal = var.geth.container.port.http, external = var.geth.host.port.http_start + idx }
        ws   = var.geth.container.port.ws == -1 ? null : { internal = var.geth.container.port.ws, external = var.geth.host.port.ws_start + idx }
        p2p  = var.geth.container.port.p2p
        raft = var.geth.container.port.raft
        qlight = 30305
      }
      graphql = var.geth.container.graphql
      ip = {
        private = cidrhost(module.helper.network_cidr, idx + 1 + 2)
        public  = "localhost"
      }
    }
  ]
  tm_networking = [for idx in local.tessera_node_indices :
    {
      image = var.tessera.container.image
      port = {
        thirdparty = { internal = var.tessera.container.port.thirdparty, external = var.tessera.host.port.thirdparty_start + idx }
        q2t        = { internal = var.tessera.container.port.q2t, external = var.tessera.host.port.q2t_start + idx }
        p2p        = var.tessera.container.port.p2p
      }
      ip = {
        private = cidrhost(module.helper.network_cidr, idx + 1 + 100)
        public  = "localhost"
      }
    }
  ]
  besu_networking = [for idx in local.besu_node_indices :
    {
      image = var.besu.container.image
      port = {
        http    = { internal = var.besu.container.port.http, external = var.besu.host.port.http_start + local.number_of_quorum_nodes + idx }
        ws      = { internal = var.besu.container.port.ws, external = var.besu.host.port.ws_start + idx }
        graphql = { internal = var.besu.container.port.graphql, external = var.besu.host.port.graphql_start + idx }
        p2p     = var.besu.container.port.p2p
      }
      ip = {
        private = cidrhost(module.helper.network_cidr, idx + 1 + 2 + var.number_of_quorum_nodes)
        public  = "localhost"
      }
    }
  ]
  ethsigner_networking = [for idx in local.besu_node_indices :
    {
      image = var.ethsigner.container.image
      port  = { internal = var.ethsigner.container.port, external = var.ethsigner.host.port_start + idx }
      ip = {
        private = cidrhost(module.helper.network_cidr, idx + 1 + 10)
        public  = "localhost"
      }
    }
  ]
}

module "helper" {
  source = "../_modules/docker-helper"

  consensus              = var.consensus
  number_of_besu_nodes   = local.number_of_besu_nodes
  number_of_quorum_nodes = local.number_of_quorum_nodes
  hybrid-network         = local.hybrid_network
  geth = {
    container = {
      image   = var.geth.container.image
      port    = { raft = 50400, p2p = 21000, qlight = 30305, http = 8545, ws = -1 }
      graphql = true
    }
    host = {
      port = { http_start = 22000, ws_start = -1 }
    }
  }
  tessera = {
    container = {
      image = var.tessera.container.image
      port  = { thirdparty = 9080, p2p = 9000, q2t = 9081, q2t = 9081 }
    }
    host = {
      port = { thirdparty_start = 9080, q2t_start = 49081 }
    }
  }
  besu = {
    container = {
      image = var.besu.container.image
      port  = { http = 8545, ws = 8546, graphql = 8547, p2p = 21000 }
    }
    host = {
      port = { http_start = 22000 + local.number_of_quorum_nodes, ws_start = 23100, graphql_start = 23200 }
    }
  }
  ethsigner = {
    container = {
      image = var.ethsigner.container.image
      port  = 8545
    }
    host = {
      port_start = 24000
    }
  }
}

module "network" {
  source = "../_modules/ignite"

  consensus                     = module.helper.consensus
  privacy_enhancements          = var.privacy_enhancements
  privacy_precompile            = var.privacy_precompile
  enable_gas_price              = var.enable_gas_price
  network_name                  = var.network_name
  geth_networking               = local.geth_networking
  tm_networking                 = local.tm_networking
  output_dir                    = var.output_dir
  qbftBlock                     = var.qbftBlock
  transition_config             = var.transition_config
  hybrid_extradata              = quorum_bootstrap_istanbul_extradata.hybrid.extradata
  hybrid_network                = local.hybrid_network
  hybrid_enodeurls              = local.enode_urls
  hybrid_network_id             = random_integer.hybrid_network_id.result
  hybrid_account_alloc          = concat(local.besu_alloc, local.quorum_alloc)
  hybrid_configuration_filename = local_file.configuration.filename
  hybrid_key_data               = slice(quorum_transaction_manager_keypair.tm.*.key_data, 0, local.number_of_quorum_nodes)
  hybrid_public_key_b64         = slice(quorum_transaction_manager_keypair.tm.*.public_key_b64, 0, local.number_of_quorum_nodes)
  hybrid_tmkeys                 = slice(data.null_data_source.meta[*].inputs.tmKeys, 0, local.number_of_quorum_nodes)
  permission_qip714Block        = { block = 0, enabled = false }
  exclude_initial_nodes         = var.exclude_initial_quorum_nodes
}

module "network-besu" {
  source = "../_modules/ignite-besu"

  consensus                     = module.helper.consensus
  network_name                  = var.network_name
  besu_networking               = local.besu_networking
  tm_networking                 = local.tm_networking
  ethsigner_networking          = local.ethsigner_networking
  output_dir                    = var.output_dir
  hybrid_extradata              = quorum_bootstrap_istanbul_extradata.hybrid.extradata
  hybrid_network                = local.hybrid_network
  hybrid_enodeurls              = local.enode_urls
  hybrid_network_id             = random_integer.hybrid_network_id.result
  hybrid_account_alloc          = concat(local.besu_alloc, local.quorum_alloc)
  hybrid_node_key               = quorum_bootstrap_node_key.besu-nodekeys-generator[*].node_key_hex
  hybrid_configuration_filename = local_file.configuration.filename
  hybrid_key_data               = slice(quorum_transaction_manager_keypair.tm.*.key_data, local.number_of_quorum_nodes, local.number_of_tessera_nodes)
  hybrid_public_key_b64         = slice(quorum_transaction_manager_keypair.tm.*.public_key_b64, local.number_of_quorum_nodes, local.number_of_tessera_nodes)
  hybrid_tmkeys                 = data.null_data_source.meta[*].inputs.tmKeys
  number_of_quorum_nodes        = local.number_of_quorum_nodes
  exclude_initial_nodes         = var.exclude_initial_besu_nodes
}

module "docker" {
  source = "../_modules/docker"

  consensus       = module.helper.consensus
  geth_networking = local.geth_networking
  tm_networking   = local.tm_networking
  network_cidr    = module.helper.network_cidr
  ethstats_ip     = module.helper.ethstat_ip
  ethstats_secret = module.helper.ethstats_secret

  network_name                = module.network.network_name
  network_id                  = random_integer.hybrid_network_id.result
  node_keys_hex               = quorum_bootstrap_node_key.quorum-nodekeys-generator[*].node_key_hex
  password_file_name          = module.network.password_file_name
  geth_datadirs               = var.remote_docker_config == null ? module.network.data_dirs : split(",", join("", null_resource.scp[*].triggers.data_dirs))
  tessera_datadirs            = var.remote_docker_config == null ? module.network.tm_dirs : split(",", join("", null_resource.scp[*].triggers.quorum_tm_dirs))
  privacy_marker_transactions = var.privacy_marker_transactions

  additional_geth_args             = { for idx in local.quorum_node_indices : idx => local.more_args }
  additional_geth_container_vol    = var.additional_quorum_container_vol
  additional_tessera_container_vol = var.additional_tessera_container_vol
  tessera_app_container_path       = var.tessera_app_container_path
  accounts_count                   = module.network.accounts_count
  start_quorum                     = var.start_quorum
  start_tessera                    = var.start_tessera
  exclude_initial_nodes            = module.network.exclude_initial_nodes
}

module "docker-besu" {
  source = "../_modules/docker-besu"

  depends_on = [module.docker]

  chainId              = random_integer.hybrid_network_id.result
  besu_networking      = local.besu_networking
  ethsigner_networking = local.ethsigner_networking
  tm_networking        = local.tm_networking
  network_cidr         = module.helper.network_cidr

  network_name           = module.network-besu.network_name
  network_id             = random_integer.hybrid_network_id.result
  node_keys_hex          = quorum_bootstrap_node_key.besu-nodekeys-generator[*].node_key_hex
  besu_datadirs          = var.remote_docker_config == null ? module.network-besu.besu_dirs : split(",", join("", null_resource.scp[*].triggers.besu_dirs))
  tessera_datadirs       = var.remote_docker_config == null ? module.network-besu.tm_dirs : split(",", join("", null_resource.scp[*].triggers.besu_tm_dirs))
  ethsigner_datadirs     = var.remote_docker_config == null ? module.network-besu.ethsigner_dirs : split(",", join("", null_resource.scp[*].triggers.ethsigner_dirs))
  keystore_files         = local.keystore_files
  keystore_password_file = module.network-besu.keystore_password_file

  tessera_app_container_path = var.tessera_app_container_path
  accounts_count             = module.network-besu.accounts_count

  hybrid_network         = local.hybrid_network
  number_of_quorum_nodes = local.number_of_quorum_nodes

  start_besu            = var.start_besu
  start_ethsigner       = var.start_ethsigner
  start_tessera         = var.start_tessera
  exclude_initial_nodes = module.network-besu.exclude_initial_nodes
}

# randomize the docker network cidr
resource "random_integer" "additional_bits" {
  max = 254
  min = 1
}
