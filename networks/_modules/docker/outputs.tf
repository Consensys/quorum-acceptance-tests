output "docker_network_name" {
  value = docker_network.quorum.name
}

output "container_geth_datadir" {
  value = local.container_geth_datadir
}

output "container_tm_datadir" {
  value = local.container_tm_datadir
}

output "quorum_containers" {
  value = docker_container.geth[*].id
  description = "List of container ids"
}

output "tessera_containers" {
  value = docker_container.tessera[*].id
  description = "List of container ids"
}