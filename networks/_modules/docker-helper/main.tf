locals {
  node_indices         = range(var.number_of_nodes) // 0-based node index
  quorum_node_indices  = var.hybrid-network ? range(var.number_of_quorum_nodes) : range(var.number_of_nodes)
  besu_node_indices    = var.hybrid-network ? range(var.number_of_besu_nodes) : range(var.number_of_nodes)
  tessera_node_indices = var.hybrid-network ? range(var.number_of_quorum_nodes + var.number_of_besu_nodes) : local.node_indices

  container_network_cidr = cidrsubnet("172.16.0.0/16", 8, random_integer.additional_bits.id)

  ethstats_ip = cidrhost(local.container_network_cidr, 2)

  geth_networking = [for idx in local.quorum_node_indices :
    {
      image = var.geth.container.image
      port = {
        http = { internal = var.geth.container.port.http, external = var.geth.host.port.http_start + idx }
        ws   = var.geth.container.port.ws == -1 ? null : { internal = var.geth.container.port.ws, external = var.geth.host.port.ws_start + idx }
        p2p  = var.geth.container.port.p2p
        raft = var.geth.container.port.raft
      }
      graphql = var.geth.container.graphql
      ip = {
        private = cidrhost(local.container_network_cidr, idx + 1 + 10)
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
        private = cidrhost(local.container_network_cidr, idx + 1 + 100)
        public  = "localhost"
      }
    }
  ]
  besu_networking = [for idx in local.besu_node_indices :
    {
      image = var.besu.container.image
      port = {
        http    = { internal = var.besu.container.port.http, external = var.besu.host.port.http_start + idx }
        ws      = { internal = var.besu.container.port.ws, external = var.besu.host.port.ws_start + idx }
        graphql = { internal = var.besu.container.port.graphql, external = var.besu.host.port.graphql_start + idx }
        p2p     = var.besu.container.port.p2p
      }
      ip = {
        private = cidrhost(local.container_network_cidr, idx + 1 + 1)
        public  = "localhost"
      }
    }
  ]
  ethsigner_networking = [for idx in local.besu_node_indices :
    {
      image = var.ethsigner.container.image
      port  = { internal = var.ethsigner.container.port, external = var.ethsigner.host.port_start + idx }
      ip = {
        private = cidrhost(local.container_network_cidr, idx + 1 + 10)
        public  = "localhost"
      }
    }
  ]
  geth_consensus_args = [for idx in local.quorum_node_indices :
    (var.consensus == "istanbul" || var.consensus == "qbft" ? "--istanbul.blockperiod 1 --syncmode full --mine --miner.threads 1" : "--raft --raftport ${local.geth_networking[idx].port.raft}")
  ]
}

# randomize the docker network cidr
resource "random_integer" "additional_bits" {
  max = 254
  min = 1
}

resource "random_id" "ethstats_secret" {
  byte_length = 16
}
