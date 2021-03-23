locals {
  publish_besu_http_ports = [for idx in local.node_indices : [
  var.besu_networking[idx].port.http]]
  publish_besu_ws_ports =  [for idx in local.node_indices : [
  var.besu_networking[idx].port.ws]]
  publish_besu_graphql_ports = [for idx in local.node_indices : [
  var.besu_networking[idx].port.graphql]]
}

resource "docker_container" "besu" {
  count = local.number_of_nodes
  name  = format("%s-besu-node%d", var.network_name, count.index)
  depends_on = [
    docker_container.tessera,
    docker_image.registry,
  docker_image.local]
  image    = var.besu_networking[count.index].image.name
  hostname = format("node%d", count.index)
  restart  = "no"
  must_run = true
  start    = true
  labels {
    label = "BesuContainer"
    value = count.index
  }
  dynamic "ports" {
    for_each = concat(local.publish_besu_http_ports[count.index], local.publish_besu_ws_ports[count.index], local.publish_besu_graphql_ports[count.index])
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
    container_path = local.container_besu_datadir_mounted
    host_path      = var.besu_datadirs[count.index]
  }
  networks_advanced {
    name         = docker_network.besu.name
    ipv4_address = var.besu_networking[count.index].ip.private
    aliases = [
    format("node%d", count.index)]
  }
  //  env = local.besu_env TODO ricardolyn: do we need env for Besu?
  healthcheck {
    test = [
      "CMD",
      "nc",
      "-vz",
      "localhost",
    var.besu_networking[count.index].port.http.internal]
    interval     = "3s"
    retries      = 10
    timeout      = "3s"
    start_period = "5s"
  }
  entrypoint = [
    "/bin/sh",
    "-c",
    <<RUN
#Besu{count.index + 1}

echo "Original files in datadir (ls ${local.container_besu_datadir})"
ls ${local.container_besu_datadir}

if [ "$ALWAYS_REFRESH" == "true" ]; then
  echo "Deleting ${local.container_besu_datadir} to refresh with original datadir"
  rm -rf ${local.container_besu_datadir}
fi

if [ ! -f "${local.container_besu_datadir}/genesis.json" ]; then
  echo "Genesis file missing. Copying mounted datadir to ${local.container_besu_datadir}"
  rm -r ${local.container_besu_datadir}
  cp -r ${local.container_besu_datadir_mounted} ${local.container_besu_datadir}
fi
echo "Current files in datadir (ls ${local.container_besu_datadir})"
ls ${local.container_besu_datadir}

# TODO ricardolyn: we need to fix the wait for tessera as `wget` is not available in the Besu container
#${local.container_besu_datadir_mounted}/wait-for-tessera.sh
exec ${local.container_besu_datadir_mounted}/start-besu.sh
RUN
  ]
  upload {
    file       = "${local.container_besu_datadir_mounted}/wait-for-tessera.sh"
    executable = true
    content    = <<EOF
#!/bin/sh

URL="${var.tm_networking[count.index].ip.private}:${var.tm_networking[count.index].port.p2p}/upcheck"

UDS_WAIT=10
for i in $(seq 1 100)
do
  result=$(wget --timeout $UDS_WAIT -qO- --proxy off $URL)
  echo "$result"
  if [ -S $PRIVATE_CONFIG ] && [ "I'm up!" = "$result" ]; then
    break
  else
    echo "Sleep $UDS_WAIT seconds. Waiting for TxManager."
    sleep $UDS_WAIT
  fi
done
EOF
  }

  upload {
    file       = "${local.container_besu_datadir_mounted}/start-besu.sh"
    executable = true
    content    = <<EOF
#!/bin/sh

if [ -f /data/qdata/cleanStorage ]; then
  echo "Cleaning besu storage"
  rm -rf ${local.container_besu_datadir}
fi

exec /opt/besu/bin/besu \
        --config-file=/config/config.toml \
        --p2p-host=$$(hostname -i) \
        --genesis-file=${local.container_besu_datadir}/genesis.json \
        --node-private-key-file=/opt/besu/keys/key \
        --min-gas-price=0 \
        --goquorum-compatibility-enabled \
        --privacy-url="${local.container_tm_q2t_urls[count.index]}" \
        --privacy-public-key-file=/config/orion/orion.pub \
        --privacy-onchain-groups-enabled=false \
        --rpc-http-api=EEA,WEB3,ETH,NET,PRIV,PERM,GOQUORUM,IBFT \
        --rpc-ws-api=EEA,WEB3,ETH,NET,PRIV,PERM,GOQUORUM,IBFT ;
EOF
  }
}
