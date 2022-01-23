locals {
  publish_ethsigner_http_ports = [for idx in local.node_indices : [
  var.ethsigner_networking[idx].port]]
}

resource "docker_container" "ethsigner" {
  count      = local.number_of_nodes
  name       = format("%s-ethsigner%d", var.network_name, count.index)
  depends_on = [docker_container.besu, docker_image.local, docker_image.registry]
  image      = var.ethsigner_networking[count.index].image.name
  hostname   = format("ethsigner_node%d", count.index)
  restart    = "no"
  must_run   = var.start_ethsigner
  start      = var.start_ethsigner
  labels {
    label = "ethsignerContainer"
    value = count.index
  }
  dynamic "ports" {
    for_each = concat(local.publish_ethsigner_http_ports[count.index])
    content {
      internal = ports.value["internal"]
      external = ports.value["external"]
    }
  }
  volumes {
    container_path = "/data"
    volume_name    = docker_volume.shared_volume[count.index].name
  }
  volumes {
    container_path = local.container_ethsigner_datadir_mounted
    host_path      = var.ethsigner_datadirs[count.index]
  }
  networks_advanced {
    name         = local.docker_network_name
    ipv4_address = var.ethsigner_networking[count.index].ip.private
    aliases = [
    format("ethsigner%d", count.index)]
  }
  healthcheck {
    test         = ["CMD", "nc", "-vz", "localhost", var.ethsigner_networking[count.index].port.internal]
    interval     = "5s"
    retries      = 60
    timeout      = "5s"
    start_period = "10s"
  }
  privileged = true
  entrypoint = [
    "/bin/sh",
    "-c",
    <<RUN
echo "EthSigner${count.index + 1}"

/opt/ethsigner/bin/ethsigner \
  --chain-id=${var.chainId} \
  --http-listen-host=0.0.0.0 \
  --downstream-http-port=${var.besu_networking[count.index].port.http.internal} \
  --downstream-http-host=${var.besu_networking[count.index].ip.private} \
  --logging=DEBUG \
  file-based-signer \
  -k ${format("%s/%s", local.container_ethsigner_datadir_mounted, var.keystore_files[count.index])} \
  -p ${format("%s/%s", local.container_ethsigner_datadir_mounted, var.keystore_password_file)}
RUN
  ]
}
