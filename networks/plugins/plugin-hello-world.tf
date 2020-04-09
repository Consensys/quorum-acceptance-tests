resource "local_file" "hello-world-config" {
  count    = var.number_of_nodes
  # file name convention is <plugin_interface_name>_config.json
  # which is being used while writing plugin-settings.json
  filename = format("%s/plugins/helloworld-config.json", module.network.data_dirs[count.index])
  content  = <<JSON
{
    "language": "en"
}
JSON
}
