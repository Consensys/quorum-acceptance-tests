resource "null_resource" "scp" {
  count      = var.remote_docker_config == null ? 0 : 1
  depends_on = [module.network]
  triggers = {
    data_dirs = join(",", module.network.data_dirs)
    tm_dirs   = join(",", module.network.tm_dirs)
  }
  connection {
    type        = "ssh"
    agent       = false
    timeout     = "60s"
    host        = var.remote_docker_config.ssh_host
    user        = var.remote_docker_config.ssh_user
    private_key = file(var.remote_docker_config.private_key_file)
  }
  provisioner "remote-exec" {
    inline = [
      "sudo mkdir -p ${module.network.generated_dir}",
      "sudo chown ${var.remote_docker_config.ssh_user} ${module.network.generated_dir}"
    ]
  }
  provisioner "file" {
    source      = module.network.generated_dir
    destination = module.network.generated_dir
  }
}