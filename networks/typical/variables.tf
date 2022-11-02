variable "consensus" {
  default = "istanbul"
}

variable "isMPS" {
  default = false
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

variable "enable_gas_price" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = false }
  description = "enable/disable gas price and set the block height at which it is enabled"
}

variable "network_name" {
  default = "typical"
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
variable "number_of_nodes" {
  default = 4
}

variable "quorum_docker_image" {
  type        = object({ name = string, local = bool })
  default     = { name = "quorumengineering/quorum:develop", local = false }
  description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
}

variable "tessera_docker_image" {
  type        = object({ name = string, local = bool })
  default     = { name = "quorumengineering/tessera:develop", local = false }
  description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
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

variable "override_named_account_allocation" {
  default     = {}
  description = <<-EOT
Override default allocation of accounts
E.g.: use 2 named account: ACC1, ACC2 for node 1
{
  0 = ["ACC1", "ACC2"]
}
EOT
}

variable "override_vnodes" {
  type        = map(object({ mpsEnabled = bool, vnodes = map(object({ name = string, tmKeys = list(string), ethKeys = list(string) })) }))
  default     = {}
  description = <<-EOT
Sets the allocations for TM & Eth keys to a Node name, sat under a particular Quorum instance
E.g.:
{
    0 = {
        mpsEnabled = true,
        vnodes = {
            VNode1 = {
                name = "Node1"
                tmKeys = ["Key1", "Key2"],
                ethKeys = ["EthKey1", "EthKey2"]
            },
            VNode2 = {
                name = "Node2"
                tmKeys = ["Key3"],
                ethKeys = ["EthKey3"]
            }
        }
    },
    1 = {
        mpsEnabled = false,
        vnodes = {
            VNode3 = {
                name = "Node5"
                tmKeys = ["Key3", "Key4"],
                ethKeys = ["EthKey4"]
            }
        }
    }
}
EOT
}

variable "additional_genesis_config" {
  default     = {}
  description = <<-EOT
Merge this config with the chain config in the genesis per node. This will override existing keys
E.g.: enable isMPS for node 1
{
  0 = {
    isMPS = true
  }
}
EOT
}

variable "additional_tessera_config" {
  default     = {}
  description = <<-EOT
Merge this config with the default config per node. This will override existing keys
E.g.: add config to node 1
{
  0 = {
    alwaysSendTo = ["xyz"]
  }
}
EOT
}

variable "qbftBlock" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = false }
  description = "qbft fork block (enabled/disabled) and the block height at which it is enabled"
}

variable "transition_config" {
  type    = object({ transitions = list(object({ block = number, algorithm = optional(string), emptyblockperiodseconds = optional(number), blockReward = optional(string), miningBeneficiary = optional(string), beneficiaryMode = optional(string)}))})
  default = { transitions = [{ "block": 110, "miningBeneficiary": "0x0638e1574728b6d862dd5d3a3e0942c3be47d996", "blockReward": "20"}, { "block": 115, "beneficiaryMode": "fixed"}, { "block": 120, "emptyblockperiodseconds": 2, "blockReward": "0xa"}, { "block": 125, "emptyblockperiodseconds": 1, "beneficiaryMode": "validators" }] }
}

variable "qlight_clients" {
  type = map(object({ server_idx = number, mps_psi = string, mt_is_server_tls_enabled = bool, mt_scope = string }))
  default = {}
  description = "Map of which nodes are qlight clients (by 0-based index) and additional config including the index of their corresponding server node"
}

variable "qlight_server_indices" {
  type = list(number)
  default = []
  description = "List of which nodes are qlight servers (by 0-based index)"
}
