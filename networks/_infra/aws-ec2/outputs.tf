locals {
  private_key_file = abspath(local_file.private_key.filename)
  host             = aws_instance.my.public_dns
  docker_host      = "tcp://${aws_instance.my.public_dns}:${local.docker_tcp_port}"
}

output "docker_host" {
  value = local.docker_host
}
