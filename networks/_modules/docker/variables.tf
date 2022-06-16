variable "network_name" {
  description = "Name of the network"
}

variable "additional_geth_args" {
  default     = {}
  description = "Additional geth args per node"
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
      qlight = number
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

variable "privacy_marker_transactions" {
  type        = bool
  description = "Enable privacy marker transactions on the node"
}

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

variable "qlight_clients" {
  type = map(object({ server_idx = number, mps_psi = string, mt_is_server_tls_enabled = bool, mt_scope = string }))
  description = "Map keys are the 0-based indexes of the nodes to be configured as qlight clients.  Map values contain additional config for the qlight client. server_idx: 0-based index of the qlclient's server node, mps_psi: the psi to connect to if qlclient's server node is using mps (use empty string if mps is disabled), mt_is_server_tls_enabled: whether the qlclient's server node has RPC security enabled (use false if mt is disabled), mt_scope: the oauth2 scope to use when fetching a token for the qlclient (use empty string if mt is disabled)"
  default = {}
}

variable "qlight_server_indices" {
  type = list(number)
  description = "List of which nodes are qlight servers (by 0-based index)"
  default = []
}

variable "qlight_p2p_urls" {
  type = list(string)
  description = "List of qlight p2p node urls"
  default = []
}

variable "oauth2_server" {
  type = object({
    start = bool
    name = string
    serve_port = object({ internal = number, external = number })
    admin_port = object({ internal = number, external = number })
  })
  default = {
    start = false
    name = "default-oauth2-server"
    serve_port = { internal = 4444, external = 4444 }
    admin_port = { internal = 4445, external = 4445 }
  }
  description = "Whether to start a hydra oauth2 server (e.g. when using the RPC API security plugin)"
}
