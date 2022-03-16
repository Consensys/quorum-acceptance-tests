locals {
  number_of_nodes                = length(var.node_keys_hex)
  container_geth_datadir         = "/data/qdata"
  container_tm_datadir           = "/data/tm"
  container_geth_datadir_mounted = "/data/qdata-mount"
  container_tm_datadir_mounted   = "/data/tm-mount"
  container_tm_ipc_file          = "/data/tm.ipc"
  container_plugin_acctdir       = "/data/plugin-accts"

  node_indices = range(local.number_of_nodes) // 0-based node index
  quorum_initial_paticipants = merge(
    { for id in local.node_indices : id => "true" }, // default to true for all
    { for id in var.exclude_initial_nodes : id => "false" }
  )
  must_start = [for idx in local.node_indices : tobool(lookup(local.quorum_initial_paticipants, idx, "false")) && tobool(var.start_quorum)]

  # support qlight - clients do not use tessera. ql-clients, ql-servers, and non-server full nodes all use unique CLI flags.
  qlight_client_indices = [for k in keys(var.qlight_clients) : parseint(k, 10)] # map keys are string type, so convert to int
  full_node_indices = tolist(setsubtract(range(local.number_of_nodes), local.qlight_client_indices)) # note: order of the resulting list is not guaranteed as we are converting from a set

  unchangeable_geth_env = {
    PRIVATE_CONFIG = local.container_tm_ipc_file
  }
  geth_env = [for k, v in merge(var.additional_geth_env, local.unchangeable_geth_env) : "${k}=${v}"]
  qlight_client_env = [for k, v in var.additional_geth_env : "${k}=${v}"]
  tm_env   = [for k, v in var.tm_env : "${k}=${v}"]
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
