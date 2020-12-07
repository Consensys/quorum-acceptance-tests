variable "consensus" {
  default = "istanbul"
}

variable "privacy_enhancements" {
    type        = object({ block = number, enabled = bool })
    default     = { block = 0, enabled = false }
    description = "privacy enhancements state (enabled/disabled) and the block height at which they are enabled"
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
  default     = { name = "amalrajmani/quorum:raft1", local = false }
  description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
}

variable "tessera_docker_image" {
    type        = object({ name = string, local = bool })
    default     = { name = "quorumengineering/tessera:latest", local = false }
    description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
}

variable "docker_registry" {
  type    = list(object({ name = string, username = string, password = string }))
  default = []
}
