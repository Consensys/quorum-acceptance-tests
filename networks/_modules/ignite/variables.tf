variable "network_name" {
  default     = ""
  description = "Name of the network being created. If emtpy, a random name will be used"
}

variable "geth_networking" {
  type = list(object({
    port = object({
      http = object({ internal = number, external = number })
      ws   = object({ internal = number, external = number })
      p2p  = number
      raft = number
    })
    graphql = bool
    ip      = object({ private = string, public = string })
  }))
  description = "Networking configuration for `geth` nodes in the network. Number of items must match `tm_networking`"
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

variable "consensus" {
  default     = "istanbul"
  description = "Consensus algorithm being used in the network. Supported values are: istanbul and raft"
}

variable "privacy_enhancements" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = false }
  description = "privacy enhancements state (enabled/disabled) and the block height at which they are enabled"
}

variable "permission_qip714Block" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 20, enabled = true }
  description = "required for testing permission model"
}

variable "privacy_precompile" {
  type        = object({ block = number, enabled = bool })
  description = "Set the privacyPrecompileBlock fork"
}

variable "output_dir" {
  default     = "build"
  description = "Target directory that contains generated resources for the network"
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

variable "exclude_initial_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) from initial list of participants. E.g: [3, 4, 5] to exclude Node4, Node5, and Node6 from the initial participants of the network"
}

variable "non_validator_nodes" {
  default     = []
  description = "Exclude nodes (0-based index) as validators. Default all nodes are validators"
}

variable "override_vnodes" {
  type        = map(object({ mpsEnabled = bool, vnodes = map(object({ name = string, tmKeys = list(string), ethKeys = list(string) })) }))
  default     = {}
  description = ""
}

variable "additional_tessera_config" {
  default     = {}
  description = <<-EOT
Merge this config with the default config per node. This takes precedence over existing keys.
E.g.: add config to node 1
{
  0 = {
    alwaysSendTo = ["xyz"]
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

variable "qbftBlock" {
  type        = object({ block = number, enabled = bool })
  default     = { block = 0, enabled = false }
  description = "qbft transition/fork block (enabled/disabled) and the block height at which it is enabled"
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
