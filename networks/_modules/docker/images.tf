data "docker_registry_image" "geth" {
  count = var.geth.container.image.local ? 0 : 1
  name  = var.geth.container.image.name
}

resource "docker_image" "geth" {
  name          = var.geth.container.image.name
  keep_locally  = var.geth.container.image.local
  pull_triggers = [coalesce(join("", data.docker_registry_image.geth[*].sha256_digest), "static")]
}

data "docker_registry_image" "tessera" {
  count = var.tessera.container.image.local ? 0 : 1
  name  = var.tessera.container.image.name
}

resource "docker_image" "tessera" {
  name          = var.tessera.container.image.name
  keep_locally  = var.tessera.container.image.local
  pull_triggers = [coalesce(join("", data.docker_registry_image.tessera[*].sha256_digest), "static")]
}

data "docker_registry_image" "ethstats" {
  count = var.ethstats.container.image.local ? 0 : 1
  name  = var.ethstats.container.image.name
}

resource "docker_image" "ethstats" {
  name          = var.ethstats.container.image.name
  keep_locally  = true
  pull_triggers = [coalesce(join("", data.docker_registry_image.ethstats[*].sha256_digest), "static")]
}