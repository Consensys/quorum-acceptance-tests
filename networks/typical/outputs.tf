output "generated_dir" {
  value = module.network.generated_dir
}

#output "debug_consensus" { value = module.helper.consensus }
#output "debug_privacy_enhancements" { value = var.privacy_enhancements }
#output "debug_privacy_precompile" { value = var.privacy_precompile }
#output "debug_network_name" { value = var.network_name }
#output "debug_geth_networking" { value = module.helper.geth_networking }
#output "debug_tm_networking" { value = module.helper.tm_networking }
#output "debug_output_dir" {value = var.output_dir }
#output "debug_qbftBlock" {  value = var.qbftBlock}
#output "debug_override_tm_named_key_allocation" { value = var.override_tm_named_key_allocation }
#output "debug_override_named_account_allocation" { value = var.override_named_account_allocation }
#output "debug_override_vnodes" { value = var.override_vnodes }
#output "debug_additional_tessera_config" { value = var.additional_tessera_config }
#output "debug_additional_genesis_config" { value = var.additional_genesis_config }
#output "debug_qlight_client_indices" { value = local.qlight_client_indices }
#output "debug_qlight_clients" { value = var.qlight_clients }
#
#output "ignite_debug_quorum_bootstrap_keystore_accountkeys_generator" {
#  value = module.network.debug_quorum_bootstrap_keystore_accountkeys_generator
#}
#
#output "ignite_debug_non_qlight_client_node_indices" {
#  value = module.network.debug_non_qlight_client_node_indices
#}
#
#output "ignite_debug_qlight_clients" {
#  value = module.network.debug_qlight_clients
#}
#
#output "ignite_debug_named_accounts_alloc" {
#  value = module.network.debug_named_accounts_alloc
#}
#
#output "ignite_debug_vnodes" {
#  value = module.network.debug_vnodes
#}
