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
      q2t        = object({ internal = number, external = number })
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
      p2p     = number
    })
    ip = object({ private = string, public = string })
  }))
  description = "Networking configuration for `besu` nodes in the network. Number of items must match `tm_networking`"
}

variable "ethsigner_networking" {
  type = list(object({
    port = object({ internal = number, external = number })
    ip   = object({ private = string, public = string })
  }))
  description = "Networking configuration for `ethsigner` nodes in the network. Number of items must match `besu_networking`"
}

variable "output_dir" {
  default     = "build"
  description = "Target directory that contains generated resources for the network"
}

variable "hybrid_extradata" {
  default     = []
  description = "Extradata for hybrid network"
}

variable "hybrid_network" {
  type        = bool
  default     = false
  description = "true if it a besu-quorum hybrid network"
}

variable "hybrid_enodeurls" {
  type        = list(string)
  default     = []
  description = "enode urls for a hybrid network"
}

variable "hybrid_network_id" {
  default     = 1500
  description = "Network Id of the hybrid network"
}

variable "hybrid_account_alloc" {
  default     = []
  description = "Account allocations for hybrid network"
}

variable "hybrid_node_key" {
  default     = []
  description = "Node keys for besu in case of hybrid network"
}

variable "hybrid_configuration_filename" {
  default = ""
  description = "Configuration filename for hybrid network"
}

variable "hybrid_tmkeys" {
  default = []
  description = "tmkeys to be used for hybrid network"
}

variable "hybrid_public_key_b64" {
  default = []
  description = "tessera public key in base64 encoding for hybrid network"
}

variable "hybrid_key_data" {
  default = []
  description = "tessera key data for hybrid network"
}

variable "number_of_quorum_nodes" {
  default = 0
  description = "Number of quorum nodes in the hybrid network"
}
