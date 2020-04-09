variable "network_name" {}
variable "consensus" {}
variable "number_of_nodes" {
  default = 4
}

variable "exclude_initial_nodes" {
  default = []
  description = "Exclude nodes (0-based index) from initial network setup"
}

variable "output_dir" {
  default = "/tmp"
}

variable "docker_registry" {
  type = list(object({ name = string, username = string, password = string }))
  default = []
  description = "List of docker registeries to pull images from"
}

variable "docker_images" {
  type = list(string)
  default = []
  description = "List of docker images for pulling"
}