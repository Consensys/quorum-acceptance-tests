locals {
  number_of_nodes                = length(var.node_keys_hex)
  container_geth_datadir         = "/data/qdata"
  container_tm_datadir           = "/data/tm"
  container_geth_datadir_mounted = "/data/qdata-mount"
  container_tm_datadir_mounted   = "/data/tm-mount"
  container_tm_ipc_file          = "/data/tm.ipc"

  node_indices              = range(local.number_of_nodes) // 0-based node index
  quorum_initial_paticipants = merge(
    { for id in local.node_indices : id => "true" }, // default to true for all
    { for id in var.exclude_initial_nodes : id => "false" }
  )
  must_start = [ for idx in local.node_indices : tobool(lookup(local.quorum_initial_paticipants, idx, "false")) && tobool(var.start_quorum) ]
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