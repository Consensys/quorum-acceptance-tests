locals {
  number_of_nodes                = length(var.node_keys_hex)
  container_besu_datadir         = "/data/besu"
  container_tm_datadir           = "/data/tm"
  container_ethsigner_datadir           = "/data/ethsigner"
  container_besu_datadir_mounted = "/data/besu-mount"
  container_tm_datadir_mounted   = "/data/tm-mount"
  container_ethsigner_datadir_mounted   = "/data/ethsigner-mount"
  container_tm_q2t_url          = ""// TODO ricardolyn

  node_indices              = range(local.number_of_nodes) // 0-based node index
  node_initial_paticipants = { for id in local.node_indices : id => "true" }, // default to true for all

  tm_env = [for k, v in var.tm_env : "${k}=${v}"]
}

resource "docker_network" "quorum" {
  name = format("%s-net", var.network_name)
  ipam_config {
    subnet = var.network_cidr
  }
}

resource "docker_volume" "shared_volume" {
  count = local.number_of_nodes
  name  = format("%s-vol%d", var.network_name, count.index)
}
