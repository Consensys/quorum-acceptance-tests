locals {
  include_helloworld = lookup(var.plugins, "helloworld", null) != null
}

resource "local_file" "hello-world-config" {
  count = local.include_helloworld ? var.number_of_nodes : 0
  # file name convention is <plugin_interface_name>-config.json
  # which is being used while writing plugin-settings.json
  filename = format("%s/plugins/helloworld-config.json", module.network.data_dirs[count.index])
  content  = <<JSON
{
    "language": "en"
}
JSON
}
