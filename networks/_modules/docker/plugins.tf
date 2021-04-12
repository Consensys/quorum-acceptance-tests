// make sure the plugin account config directories exist for each node
resource "local_file" "plugin_acct_dir_files" {
  count    = length(var.host_plugin_account_dirs)
  filename = "${var.host_plugin_account_dirs[count.index]}/tmp"
  content  = "{}"
}

// if no plugin account dirs have been provided then create a set of fallback dirs that will not be used but are still
// required for mounting to the geth containers
resource "local_file" "plugin_acct_fallback_dir_files" {
  count    = length(var.host_plugin_account_dirs) == 0 ? local.number_of_nodes : 0
  filename = "/tmp/quorum-plugin-accts-fallback-${count.index}"
  content  = "{}"
}
