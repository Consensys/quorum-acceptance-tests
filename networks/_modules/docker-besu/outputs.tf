output "docker_network_name" {
  value = local.docker_network_name
}

output "container_besu_datadir" {
  value = local.container_besu_datadir
}

output "container_tm_datadir" {
  value = local.container_tm_datadir
}

output "container_ethsigner_datadir" {
  value = local.container_besu_datadir_mounted
}

output "besu_containers" {
  value       = docker_container.besu[*].id
  description = "List of container ids"
}

output "tessera_containers" {
  value       = docker_container.tessera[*].id
  description = "List of container ids"
}

output "ethsigner_containers" {
  value       = docker_container.ethsigner[*].id
  description = "List of container ids"
}
