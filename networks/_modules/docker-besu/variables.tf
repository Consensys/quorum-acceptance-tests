variable "chainId" {
  description = "ChainID of the network"
}

variable "network_name" {
  description = "Name of the network"
}

variable "besu_datadirs" {
  type        = list(string)
  description = "List of Besu working directories"
}

variable "tessera_datadirs" {
  type        = list(string)
  description = "List of Tessera working directories"
}

variable "ethsigner_datadirs" {
  type        = list(string)
  description = "List of EthSigner working directories"
}

variable "keystore_files" {
  type        = list(string)
  description = "List of Keystores"
}

variable "keystore_password_file" {
  type        = string
}

variable "node_keys_hex" {
  type        = list(string)
  description = "List of node keys in hex"
}

variable "besu_networking" {
  type = list(object({
    image = object({ name = string, local = bool })
    port = object({
      http = object({ internal = number, external = number })
      ws = object({ internal = number, external = number })
      graphql = object({ internal = number, external = number })
      p2p = number
    })
    ip      = object({ private = string, public = string })
  }))
  description = "Networking configuration for `besu` nodes in the network. Number of items must match `besu_networking`"
}

variable "ethsigner_networking" {
  type = list(object({
    image = object({ name = string, local = bool })
    port  = object({ internal = number, external = number })
    ip      = object({ private = string, public = string })
  }))
  description = "Networking configuration for `ethsigner` nodes in the network. Number of items must match `ethsigner_networking`"
}

variable "tm_networking" {
  type = list(object({
    image = object({ name = string, local = bool })
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


variable "network_id" {}

variable "network_cidr" {}

variable "tm_env" {
  type        = map(string)
  default     = {}
  description = "Environment variables for each `tessera` node in the network, provided as a key/value map."
}

variable "tessera_app_container_path" {
  type        = map(string)
  default     = {}
  description = "Path to Tessera app jar file in the container. Each map key is the node index (0-based)"
}

variable "accounts_count" {
}
