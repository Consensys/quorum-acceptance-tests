output "network_id" {
  value = random_integer.network_id.result
}

output "generated_dir" {
  value = quorum_bootstrap_network.this.network_dir_abs
}

output "data_dirs" {
  value = quorum_bootstrap_data_dir.datadirs-generator[*].data_dir_abs
}

output "tm_dirs" {
  value = [for id in local.node_indices : format("%s/%s%s", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, id)]
}

output "node_keys_hex" {
  value = quorum_bootstrap_node_key.nodekeys-generator[*].node_key_hex
}

output "password_file_name" {
  value = local.password_file
}

output "network_name" {
  value = local.network_name
}

output "application_yml_file" {
  value = local.hybrid_network ? var.hybrid_configuration_filename : local_file.configuration[0].filename
}

output "exclude_initial_nodes" {
  value = var.exclude_initial_nodes
}

output "accounts_count" {
  value = { for id in local.node_indices : id => length(local.named_accounts_alloc[id]) }
}

output "qlight_p2p_urls" {
  value = local.qlight_p2p_urls
}
