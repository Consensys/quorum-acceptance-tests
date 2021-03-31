locals {
  number_of_nodes                = length(var.node_keys_hex)
  container_besu_datadir         = "/opt/besu/data"
  container_tm_datadir           = "/data/tm"
  container_besu_datadir_mounted = "/opt/besu/mount"
  container_tm_datadir_mounted   = "/data/tm-mount"
  container_ethsigner_datadir_mounted   = "/data/ethsigner"
  container_tm_q2t_urls = [for idx in local.node_indices :
    "http://${var.tm_networking[idx].ip.private}:${var.tm_networking[idx].port.q2t.internal}"]

  node_indices              = range(local.number_of_nodes) // 0-based node index
  node_initial_paticipants = { for id in local.node_indices : id => "true" } // default to true for all

  tm_env = [for k, v in var.tm_env : "${k}=${v}"]
}

resource "docker_network" "besu" {
  name = format("%s-net", var.network_name)
  ipam_config {
    subnet = var.network_cidr
  }
}

resource "docker_volume" "shared_volume" {
  count = local.number_of_nodes
  name  = format("%s-vol%d", var.network_name, count.index)
}
