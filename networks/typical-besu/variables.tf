variable "consensus" {
  default = "ibft2"
}

variable "network_name" {
  default = "typical-besu"
}

variable "output_dir" {
  default = "/tmp/besu"
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

variable "besu_docker_image" {
  type        = object({ name = string, local = bool })
  default     = { name = "hyperledger/besu:develop", local = false }
  description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
}

variable "tessera_docker_image" {
  type        = object({ name = string, local = bool })
  default     = { name = "quorumengineering/tessera:develop", local = false }
  description = "Local=true indicates that the image is already available locally and don't need to pull from registry"
}

variable "ethsigner_docker_image" {
  type        = object({ name = string, local = bool })
  default     = { name = "consensys/ethsigner:develop", local = false }
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
