output "docker_network_name" {
  value = docker_network.quorum.name
}

output "docker_network_ipam_config" {
  value = docker_network.quorum.ipam_config
}

output "container_geth_datadir" {
  value = local.container_geth_datadir
}

output "container_tm_datadir" {
  value = local.container_tm_datadir
}

output "quorum_containers" {
  value       = docker_container.geth[*].id
  description = "List of container ids"
}

output "tessera_containers" {
  value       = docker_container.tessera[*].id
  description = "List of container ids"
}



### DEBUG ###
output "debug_qlight_clients" {
  value = var.qlight_clients
}

output "debug_qlight_client_indices" {
  value = local.qlight_client_indices
}

output "debug_qlight_server_indices" {
  value = local.qlight_server_indices
}

output "debug_full_node_indices" {
  value = local.full_node_indices
}

output "debug_all_node_indices" {
  value = range(local.number_of_nodes)
}
