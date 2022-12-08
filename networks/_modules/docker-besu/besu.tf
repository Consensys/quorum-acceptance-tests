locals {
  publish_besu_http_ports = [for idx in local.node_indices : [
  var.besu_networking[idx].port.http]]
  publish_besu_ws_ports = [for idx in local.node_indices : [
  var.besu_networking[idx].port.ws]]
  publish_besu_graphql_ports = [for idx in local.node_indices : [
  var.besu_networking[idx].port.graphql]]
}

resource "docker_container" "besu" {
  count      = local.number_of_nodes
  name       = format("%s-node%d", var.network_name, local.tessera_node_indices[count.index])
  depends_on = [docker_container.tessera, docker_image.registry, docker_image.local]
  image      = var.besu_networking[count.index].image.name
  hostname   = format("node%d", count.index)
  restart    = "no"
  must_run   = local.must_start[count.index]
  start      = local.must_start[count.index]
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
    name         = local.docker_network_name
    ipv4_address = var.besu_networking[count.index].ip.private
    aliases = [
    format("node%d", count.index)]
  }
  entrypoint = [
    "/bin/sh",
    "-c",
    <<RUN
echo "Besu{count.index + 1}"

echo "Original files in datadir (ls ${local.container_besu_datadir})"
ls ${local.container_besu_datadir}

if [ "$ALWAYS_REFRESH" == "true" ]; then
  echo "Deleting ${local.container_besu_datadir} to refresh with original datadir"
  rm -rf ${local.container_besu_datadir} || true
fi

if [ ! -f "${local.container_besu_datadir}/genesis.json" ]; then
  echo "Genesis file missing. Copying mounted datadir to ${local.container_besu_datadir}"
  rm -r ${local.container_besu_datadir} || true
  cp -r ${local.container_besu_datadir_mounted} ${local.container_besu_datadir}
fi
echo "Current files in datadir (ls ${local.container_besu_datadir})"
ls ${local.container_besu_datadir}

exec ${local.container_besu_datadir}/start-besu.sh
RUN
  ]

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
        --config-file=${local.container_besu_datadir}/config.toml \
        --p2p-host=${var.besu_networking[count.index].ip.private} \
        --p2p-port=${var.besu_networking[count.index].port.p2p} \
        --genesis-file=${local.container_besu_datadir}/genesis.json \
        --node-private-key-file=${local.container_besu_datadir}/key \
        --revert-reason-enabled=true \
        --min-gas-price=0 \
        --privacy-url="${local.container_tm_q2t_urls[count.index]}" \
        --privacy-public-key-file=${local.container_besu_datadir}/tmkey.pub \
        --privacy-onchain-groups-enabled=false \
        --tx-pool-limit-by-account-percentage=0.5 \
%{if var.hybrid_network~}
        --rpc-http-api=ADMIN,EEA,WEB3,ETH,MINER,NET,PRIV,PERM,GOQUORUM,QBFT \
        --rpc-ws-api=ADMIN,EEA,WEB3,ETH,MINER,NET,PRIV,PERM,GOQUORUM,QBFT ;
%{else~}
        --rpc-http-api=ADMIN,EEA,WEB3,ETH,MINER,NET,PRIV,PERM,GOQUORUM,IBFT \
        --rpc-ws-api=ADMIN,EEA,WEB3,ETH,MINER,NET,PRIV,PERM,GOQUORUM,IBFT ;
%{endif~}
EOF
  }
}
