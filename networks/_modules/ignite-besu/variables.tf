variable "network_name" {
  default     = ""
  description = "Name of the network being created. If empty, a random name will be used"
}

variable "consensus" {
  default     = "ibft2"
  description = "Consensus algorithm being used in the network. Supported values are: ibft2"
}

variable "tm_networking" {
  type = list(object({
    port = object({
      thirdparty = object({ internal = number, external = number })
      q2t = object({ internal = number, external = number })
      p2p        = number
    })
    ip = object({
      private = string
      public  = string
    })
  }))
  description = "Networking configuration for `tessera` nodes in the network. Number of items must match `geth_networking`"
}

variable "besu_networking" {
  type = list(object({
    port = object({
      http    = object({ internal = number, external = number })
      ws      = object({ internal = number, external = number })
      graphql = object({ internal = number, external = number })
      p2p = number
    })
    ip = object({ private = string, public = string })
  }))
  description = "Networking configuration for `besu` nodes in the network. Number of items must match `tm_networking`"
}

variable "ethsigner_networking" {
  type = list(object({
    port  = object({ internal = number, external = number })
    ip    = object({ private = string, public = string })
  }))
  description = "Networking configuration for `ethsigner` nodes in the network. Number of items must match `besu_networking`"
}

variable "output_dir" {
  default     = "build"
  description = "Target directory that contains generated resources for the network"
}
