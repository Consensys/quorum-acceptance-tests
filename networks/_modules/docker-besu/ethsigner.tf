locals {
  publish_ethsigner_http_ports = [for idx in local.node_indices : [
  var.ethsigner_networking[idx].port]]
}

resource "docker_container" "ethsigner" {
  count = local.number_of_nodes
  name  = format("%s-ethsigner-node%d", var.network_name, count.index)
  depends_on = [
    docker_container.besu,
    docker_image.registry,
  docker_image.local]
  image    = var.ethsigner_networking[count.index].image.name
  hostname = format("ethsigner_node%d", count.index)
  restart  = "no"
  must_run = true
  start    = true
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
    name         = docker_network.besu.name
    ipv4_address = var.ethsigner_networking[count.index].ip.private
    aliases = [
    format("ethsigner_node%d", count.index)]
  }
  //  env = local.ethsigner_env TODO ricardolyn: do we need env for ethsigner?
  healthcheck {
    test = [
      "CMD",
      "nc",
      "-vz",
      "localhost",
    var.ethsigner_networking[count.index].port.internal]
    interval     = "3s"
    retries      = 10
    timeout      = "3s"
    start_period = "5s"
  }
  command = [
    "--chain-id=${var.chainId}",
    "--http-listen-host=0.0.0.0",
    "--downstream-http-port=${var.besu_networking[count.index].port.http.internal}",
    "--downstream-http-host=${format("node%d", count.index)}",
    "--logging=TRACE",
    "file-based-signer",
    "-k",
    "/data/ethsigner/keyfile",
    // TODO ricardolyn
    "-p",
    "/data/ethsigner/passwordfile",
    // TODO ricardolyn
  ]
}
