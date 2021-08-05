output "chainId" {
  value = local.chainId
}

output "network_id" {
  value = random_integer.network_id.result
}

output "generated_dir" {
  value = quorum_bootstrap_network.this.network_dir_abs
}

output "besu_dirs" {
  value = local.besu_dirs
}

output "tm_dirs" {
  value = [for id in local.node_indices : format("%s/%s%s", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, id + local.number_of_quorum_nodes)]
}

output "ethsigner_dirs" {
  value = [for id in local.node_indices : format("%s/%s%s", quorum_bootstrap_network.this.network_dir_abs, local.ethsigner_dir_prefix, id)]
}

output "node_keys_hex" {
  value = quorum_bootstrap_node_key.nodekeys-generator[*].node_key_hex
}

output "network_name" {
  value = local.network_name
}

output "application_yml_file" {
  value = local.hybrid_network ? var.hybrid_configuration_filename : local_file.configuration[0].filename
}

output "accounts_count" {
  value = { for id in local.node_indices : id => length(local.named_accounts_alloc[id]) }
}

output "keystore_files" {
  value = local.hybrid_network ? [] : [for idx in local.node_indices : format("%s/%s", local.keystore_folder, regex("UTC.+$", quorum_bootstrap_keystore.accountkeys-generator[idx].account[0].account_url))]
}

output "keystore_password_file" {
  value = local.keystore_password_file
}
