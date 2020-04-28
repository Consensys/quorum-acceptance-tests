locals {
  node_indices = range(var.number_of_nodes) // 0-based node index

  container_network_cidr = cidrsubnet("172.16.0.0/16", 8, random_integer.additional_bits.id)

  ethstats_ip = cidrhost(local.container_network_cidr, 2)

  geth_networking = [for idx in local.node_indices :
    {
      port = {
        http = { internal = var.geth.container.port.http, external = var.geth.host.port.http_start + idx }
        ws   = { internal = var.geth.container.port.ws, external = var.geth.host.port.ws_start == -1 ? -1 : var.geth.host.port.ws_start + idx }
        p2p  = var.geth.container.port.p2p
        raft = var.geth.container.port.raft
      }
      ip = {
        private = cidrhost(local.container_network_cidr, idx + 1 + 10)
        public  = "localhost"
      }
    }
  ]
  tm_networking = [for idx in local.node_indices :
    {
      port = {
        thirdparty = { internal = var.tessera.container.port.thirdparty, external = var.tessera.host.port.thirdparty_start + idx }
        p2p        = var.tessera.container.port.p2p
      }
      ip = {
        private = cidrhost(local.container_network_cidr, idx + 1 + 100)
        public  = "localhost"
      }
    }
  ]
  geth_consensus_args = [for idx in local.node_indices :
    (var.consensus == "istanbul" ? "--istanbul.blockperiod 1 --syncmode full --mine --minerthreads 1" : "--raft --raftport ${local.geth_networking[idx].port.raft}")
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
