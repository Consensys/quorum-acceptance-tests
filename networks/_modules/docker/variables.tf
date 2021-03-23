variable "network_name" {
  description = "Name of the network"
}

variable "additional_geth_args" {
  default     = ""
  description = "Additional geth args for all nodes"
}

variable "geth_datadirs" {
  type        = list(string)
  description = "List of node's datadirs"
}

variable "tessera_datadirs" {
  type        = list(string)
  description = "List of Tessera working directories"
}

variable "node_keys_hex" {
  type        = list(string)
  description = "List of node keys in hex"
}

variable "geth_networking" {
  type = list(object({
    image = object({ name = string, local = bool })
    port = object({
      http = object({ internal = number, external = number })
      ws   = object({ internal = number, external = number })
      p2p  = number
      raft = number
    })
    ip      = object({ private = string, public = string })
    graphql = bool
  }))
  description = "Networking configuration for `geth` nodes in the network. Number of items must match `tm_networking`"
}

variable "tm_networking" {
  type = list(object({
    image = object({ name = string, local = bool })
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

variable "ethstats" {
  type = object({
    container = object({
      image = object({ name = string, local = bool })
      port  = number
    })
    host = object({ port = number })
  })
  default = {
    container = {
      image = { name = "puppeth/ethstats:latest", local = false },
      port  = 3000
    }
    host = { port = 3000 }
  }
}

variable "enable_ethstats" {
  default = false
}

variable "start_quorum" {
  default = true
}

variable "start_tessera" {
  default = true
}

variable "exclude_initial_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) from initial list of participants. E.g: [3, 4, 5] to exclude Node4, Node5, and Node6 from the initial participants of the network"
}

variable "consensus" {}

variable "network_id" {}

variable "ethstats_secret" {}

variable "ethstats_ip" {}

variable "password_file_name" {}

variable "network_cidr" {}

variable "additional_geth_env" {
  type        = map(string)
  default     = {}
  description = "Additional environment variables for each `geth` node in the network, provided as a key/value map.  The correct PRIVATE_CONFIG is already set by this module, so any PRIVATE_CONFIG value provided in this map will be ignored."
}

variable "tm_env" {
  type        = map(string)
  default     = {}
  description = "Environment variables for each `tessera` node in the network, provided as a key/value map."
}

variable "host_plugin_account_dirs" {
  type        = list(string)
  description = "Path to dirs on host used for sharing data for account plugin between host and containers"
  default     = []
}

variable "additional_geth_container_vol" {
  type        = map(list(object({ container_path = string, host_path = string })))
  default     = {}
  description = "Additional volume mounts for geth container. Each map key is the node index (0-based)"
}

variable "additional_tessera_container_vol" {
  type        = map(list(object({ container_path = string, host_path = string })))
  default     = {}
  description = "Additional volume mounts for tessera container. Each map key is the node index (0-based)"
}

variable "tessera_app_container_path" {
  type        = map(string)
  default     = {}
  description = "Path to Tessera app jar file in the container. Each map key is the node index (0-based)"
}

variable "accounts_count" {
}
