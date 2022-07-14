terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "2.11.0"
    }
    local = {
      source = "hashicorp/local"
    }
  }
  experiments = [ module_variable_optional_attrs ]
  required_version = ">= 0.13"
}
