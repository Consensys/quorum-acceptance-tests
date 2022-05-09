output "generated_dir" {
  value = module.network.generated_dir
}

#output "debug_vnodes" {
#  value = module.network.debug_vnodes
#}

output "debug_accounts_count" {
  value = module.network.accounts_count
}

output "debug_named_accounts_alloc" {
  value = module.network.debug_named_accounts_alloc
}

output "debug_keystore_dirs" {
  value = module.network.debug_keystore_dirs
}
