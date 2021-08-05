terraform {
  required_providers {
    quorum = {
      source  = "ConsenSys/quorum"
      version = "0.3.0"
    }
    docker = {
      source  = "kreuzwerker/docker"
      version = "2.11.0"
    }
    local = {
      source = "hashicorp/local"
    }
  }
  required_version = ">= 0.13"
}
