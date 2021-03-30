variable "number_of_nodes" {
  description = "Number of nodes in the network"
}

variable "consensus" {
  description = "Consensus algorithm being used in the network. Supported values are: for GoQuorum - istanbul and raft, for Besu - ibft2"
}

variable "geth" {
  type = object({
    container = object({
      image   = object({ name = string, local = bool })
      port    = object({ raft = number, p2p = number, http = number, ws = number })
      graphql = bool
    })
    host = object({
      port = object({ http_start = number, ws_start = number })
    })
  })
  default = {
    container = {
      image   = { name = "quorumengineering/quorum:latest", local = false }
      port    = { raft = 50400, p2p = 21000, http = 8545, ws = -1 }
      graphql = false
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
      port  = object({ thirdparty = number, p2p = number, q2t = number })
    })
    host = object({
      port = object({ thirdparty_start = number, q2t_start = number })
    })
  })
  default = {
    container = {
      image = { name = "quorumengineering/tessera:latest", local = false }
      port  = { thirdparty = 9080, p2p = 9000, q2t = 9081 }
    }
    host = {
      port = { thirdparty_start = 24000, q2t_start = 24100 }
    }
  }
  description = "tessera Docker container configuration"
}

variable "besu" {
  type = object({
    container = object({
      image = object({ name = string, local = bool })
      port  = object({ http = number, ws = number,graphql = number, p2p = number })
    })
    host = object({
      port = object({ http_start = number,ws_start = number,graphql_start = number })
    })
  })
  default = {
    container = {
      image = { name = "hyperledger/besu:latest", local = false }
      port  = { http = 8545, ws= 8546, graphql= 8547, p2p= 30303 }
    }
    host = {
      port = { http_start = 23000 , ws_start= 23100, graphql_start= 23200 }
    }
  }
  description = "besu Docker container configuration "
}

variable "ethsigner" {
  type = object({
    container = object({
      image = object({ name = string, local = bool })
      port  = number
    })
    host = object({
      port_start = number
    })
  })
  default = {
    container = {
      image = { name = "consensys/quorum-ethsigner:latest", local = false }
      port  = 8545
    }
    host = {
      port_start = 22000
    }
  }
  description = "ethsigner Docker container configuration "
}
