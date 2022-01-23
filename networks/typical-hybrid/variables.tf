variable "consensus" {
  default = "qbft"
}

variable "privacy_enhancements" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = false }
  description = "privacy enhancements state (enabled/disabled) and the block height at which they are enabled"
}

variable "privacy_precompile" {
  type        = object({ block = number, enabled = bool })
  default     = { enabled = false, block = 0 }
  description = "Set the privacyPrecompileBlock fork"
}

variable "privacy_marker_transactions" {
  type        = bool
  default     = false
  description = "Enable privacy marker transactions on the node"
}

variable "network_name" {
  default = "typical-hybrid"
}

variable "output_dir" {
  default = "/tmp"
}

variable "remote_docker_config" {
  type        = object({ ssh_user = string, ssh_host = string, private_key_file = string, docker_host = string })
  default     = null
  description = "Configuration to connect to a VM which enables remote docker API"
}

variable "properties_outdir" {
  default     = ""
  description = "Output directory containing DockerWaitMain-network.properties"
}

variable "gauge_env_outdir" {
  default     = ""
  description = "Output directory containing user.properties for Gauge env"
}

//---------- advanced inputs -----------
variable "number_of_quorum_nodes" {
  description = "Number of quorum nodes"
  default     = 2
}

variable "number_of_besu_nodes" {
  description = "Number of besu nodes"
  default     = 2
}

variable "hybrid-network" {
  type        = bool
  default     = true
  description = "true if it a besu-quorum hybrid network"
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
      image   = { name = "quorumengineering/quorum:develop", local = true }
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
      port  = object({ http = number, ws = number, graphql = number, p2p = number })
    })
    host = object({
      port = object({ http_start = number, ws_start = number, graphql_start = number })
    })
  })
  default = {
    container = {
      image = { name = "hyperledger/besu:develop", local = false }
      port  = { http = 8545, ws = 8546, graphql = 8547, p2p = 21000 }
    }
    host = {
      port = { http_start = 22000, ws_start = 23100, graphql_start = 23200 }
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
      image = { name = "consensys/ethsigner:develop", local = false }
      port  = 8545
    }
    host = {
      port_start = 25000
    }
  }
  description = "ethsigner Docker container configuration "
}

variable "docker_registry" {
  type    = list(object({ name = string, username = string, password = string }))
  default = []
}

variable "additional_quorum_container_vol" {
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

variable "qbftBlock" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = true }
  description = "qbft fork block (enabled/disabled) and the block height at which it is enabled"
}

variable "override_tm_named_key_allocation" {
  default     = {}
  description = <<-EOT
Override default allocation of transaction management named public key
E.g.: use 2 named keys: A1, A2 for node 1
{
  0 = ["A1", "A2"]
}
EOT
}

variable "exclude_initial_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) from initial list of participants. E.g: [3, 4, 5] to exclude Node4, Node5, and Node6 from the initial participants of the network"
}

variable "exclude_initial_besu_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) from initial list of besu participants. E.g: [3, 4, 5] to exclude Node4, Node5, and Node6 from the initial participants of the network"
}

variable "exclude_initial_quorum_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) from initial list of quorum participants. E.g: [3, 4, 5] to exclude Node4, Node5, and Node6 from the initial participants of the network"
}

variable "start_quorum" {
  default = true
}

variable "start_tessera" {
  default = true
}

variable "start_besu" {
  default = true
}

variable "start_ethsigner" {
  default = true
}

variable "template_network" {
  description = "variable to represent a template network"
  default     = false
}
