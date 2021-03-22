locals {
  all_images      = concat(distinct(var.besu_networking[*].image), distinct(var.tm_networking[*].image), distinct(var.ethsigner_networking[*].image))
  registry_images = [for img in local.all_images : img.name if img.local == "false"]
  local_images    = [for img in local.all_images : img.name if img.local == "true"]
}

data "docker_registry_image" "img" {
  count = length(local.registry_images)
  name  = local.registry_images[count.index]
}

resource "docker_image" "registry" {
  count         = length(local.registry_images)
  name          = local.registry_images[count.index]
  keep_locally  = false
  pull_triggers = [data.docker_registry_image.img[count.index].sha256_digest]
}

resource "docker_image" "local" {
  count        = length(local.local_images)
  name         = local.local_images[count.index]
  keep_locally = true
}
