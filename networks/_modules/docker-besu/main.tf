locals {
  number_of_nodes                     = length(var.node_keys_hex)
  docker_network_name                 = format("%s-net", var.network_name)
  container_besu_datadir              = "/opt/besu/data"
  container_tm_datadir                = "/data/tm"
  container_besu_datadir_mounted      = "/opt/besu/mount"
  container_tm_datadir_mounted        = "/data/tm-mount"
  container_ethsigner_datadir_mounted = "/data/ethsigner"
  container_tm_q2t_urls = [for idx in local.node_indices :
  "http://${var.tm_networking[idx].ip.private}:${var.tm_networking[idx].port.q2t.internal}"]

  node_indices             = range(local.number_of_nodes) // 0-based node index
  tessera_node_indices     = var.hybrid-network ? [for id in range(local.number_of_nodes) : sum([id, local.number_of_nodes])] : range(local.number_of_nodes)
  node_initial_paticipants = { for id in local.node_indices : id => "true" } // default to true for all

  tm_env = [for k, v in var.tm_env : "${k}=${v}"]
}

resource "docker_network" "besu" {
  count = var.hybrid-network ? 0 : 1
  name  = local.docker_network_name
  ipam_config {
    subnet = var.network_cidr
  }
}

resource "docker_volume" "shared_volume" {
  count = local.number_of_nodes
  name  = format("%s-vol%d", var.network_name, var.hybrid-network ? local.number_of_nodes + count.index : count.index)
}
