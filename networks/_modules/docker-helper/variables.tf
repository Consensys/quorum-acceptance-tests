variable "number_of_nodes" {
  description = "Number of nodes in the network"
}

variable "consensus" {
  description = "Consensus algorithm being used in the network. Supported values are: istanbul and raft"
}

variable "geth" {
  type = object({
    container = object({
      image = object({ name = string, local = bool })
      port  = object({ raft = number, p2p = number, http = number, ws = number })
    })
    host = object({
      port = object({ http_start = number, ws_start = number })
    })
  })
  default = {
    container = {
      image = { name = "quorumengineering/quorum:2.5.0", local = false }
      port  = { raft = 50400, p2p = 21000, http = 8545, ws = -1 }
    }
    host = {
      port = { http_start = 22000, ws_start = -1 }
    }
  }
  description = "geth Docker container configuration "
}

variable "tessera" {
  type = object({
    container = object({
      image = object({ name = string, local = bool })
      port  = object({ thirdparty = number, p2p = number })
    })
    host = object({
      port = object({ thirdparty_start = number })
    })
  })
  default = {
    container = {
      image = { name = "quorumengineering/tessera:0.10.3", local = false }
      port  = { thirdparty = 9080, p2p = 9000 }
    }
    host = {
      port = { thirdparty_start = 9080 }
    }
  }
  description = "tessera Docker container configuration"
}