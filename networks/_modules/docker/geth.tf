locals {
  publish_http_ports = [for idx in local.node_indices : [
  var.geth_networking[idx].port.http]]
  publish_ws_ports = var.geth_networking[0].port.ws == null ? [for idx in local.node_indices : []] : [for idx in local.node_indices : [
  var.geth_networking[idx].port.ws]]
}

resource "docker_container" "geth" {
  count      = local.number_of_nodes
  name       = format("%s-node%d", var.network_name, count.index)
  depends_on = [docker_container.ethstats, docker_image.registry, docker_image.local]
  image      = var.geth_networking[count.index].image.name
  hostname   = format("node%d", count.index)
  restart    = "no"
  must_run   = local.must_start[count.index]
  start      = local.must_start[count.index]
  labels {
    label = "QuorumContainer"
    value = count.index
  }
  ports {
    internal = var.geth_networking[count.index].port.p2p
  }
  ports {
    internal = var.geth_networking[count.index].port.raft
  }
  dynamic "ports" {
    for_each = concat(local.publish_http_ports[count.index], local.publish_ws_ports[count.index])
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
    container_path = local.container_geth_datadir_mounted
    host_path      = var.geth_datadirs[count.index]
  }
  volumes {
    container_path = local.container_plugin_acctdir
    host_path      = length(local_file.plugin_acct_dir_files) != 0 ? dirname(local_file.plugin_acct_dir_files[count.index].filename) : dirname(local_file.plugin_acct_fallback_dir_files[count.index].filename)
  }
  dynamic "volumes" {
    for_each = lookup(var.additional_geth_container_vol, count.index, [])
    content {
      container_path = volumes.value["container_path"]
      host_path      = volumes.value["host_path"]
    }
  }
  networks_advanced {
    name         = docker_network.quorum.name
    ipv4_address = var.geth_networking[count.index].ip.private
    aliases      = [format("node%d", count.index)]
  }
  env = local.geth_env
  healthcheck {
    test         = ["CMD", "nc", "-vz", "localhost", var.geth_networking[count.index].port.http.internal]
    interval     = "3s"
    retries      = 10
    timeout      = "3s"
    start_period = "5s"
  }
  entrypoint = [
    "/bin/sh",
    "-c",
    <<RUN
#Quorum${count.index + 1}

echo "Original files in datadir (ls ${local.container_geth_datadir})"
ls ${local.container_geth_datadir}

if [ "$ALWAYS_REFRESH" == "true" ]; then
  echo "Deleting ${local.container_geth_datadir} to refresh with original datadir"
  rm -rf ${local.container_geth_datadir}
fi

if [ ! -f "${local.container_geth_datadir}/genesis.json" ]; then
  echo "Genesis file missing. Copying mounted datadir to ${local.container_geth_datadir}"
  rm -r ${local.container_geth_datadir}
  cp -r ${local.container_geth_datadir_mounted} ${local.container_geth_datadir}
fi
echo "Current files in datadir (ls ${local.container_geth_datadir})"
ls ${local.container_geth_datadir}

echo "ls ${local.container_plugin_acctdir}"
ls ${local.container_plugin_acctdir}
echo "Deleting any files in ${local.container_plugin_acctdir}"
rm ${local.container_plugin_acctdir}/*
echo "ls ${local.container_plugin_acctdir}"
ls ${local.container_plugin_acctdir}
${local.container_geth_datadir_mounted}/wait-for-tessera.sh
exec ${local.container_geth_datadir_mounted}/start-geth.sh
RUN
  ]
  upload {
    file       = "${local.container_geth_datadir_mounted}/wait-for-tessera.sh"
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
    file       = "${local.container_geth_datadir_mounted}/start-geth.sh"
    executable = true
    content    = <<EOF
#!/bin/sh

if [ -f /data/qdata/cleanStorage ]; then
  echo "Cleaning geth storage"
  rm -rf /data/qdata/geth /data/qdata/quorum-raft-state /data/qdata/raft-snap /data/qdata/raft-wal /data/qdata/cleanStorage
fi

if [ -f /data/qdata/executempsdbupgrade ]; then
  echo "Executing mpsdbupgrade"
  geth --datadir ${local.container_geth_datadir} mpsdbupgrade
  rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
  rm /data/qdata/executempsdbupgrade
fi

VERSION=$(geth version | grep Quorum | cut -d ':' -f2 | xargs echo -n)

geth --datadir ${local.container_geth_datadir} init ${local.container_geth_datadir}/genesis.json

#exit if geth init fails
rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi

echo $VERSION
if [[ $VERSION == '2.5.0' ]]; then
  echo "Using --rpc flags"
  HTTP_ARGS="--rpc \
  --rpcaddr 0.0.0.0 \
  --rpcport ${var.geth_networking[count.index].port.http.internal} \
  --rpcapi admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,quorumPermission,quorumExtension,${(var.consensus == "istanbul" || var.consensus == "qbft" ? "istanbul" : "raft")} "
else
  echo "Using --http flags"
  HTTP_ARGS="--http \
  --http.addr 0.0.0.0 \
  --http.port ${var.geth_networking[count.index].port.http.internal} \
  --http.api admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,quorumPermission,quorumExtension,${(var.consensus == "istanbul" || var.consensus == "qbft" ? "istanbul" : "raft")} "
fi


exec geth \
  --identity Node${count.index + 1} \
  --datadir ${local.container_geth_datadir} \
  --nodiscover \
  --verbosity 5 \
  --networkid ${var.network_id} \
  --nodekeyhex ${var.node_keys_hex[count.index]} \
  $HTTP_ARGS \
%{if var.privacy_marker_transactions~}
  --privacymarker.enable \
%{endif~}
%{if var.geth_networking[count.index].port.ws != null~}
  --ws \
  --wsaddr 0.0.0.0 \
  --wsport ${var.geth_networking[count.index].port.ws.internal} \
  --wsapi admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,${var.consensus} \
%{endif~}
%{if var.geth_networking[count.index].graphql~}
  --graphql \
%{endif~}
  --port ${var.geth_networking[count.index].port.p2p} \
%{if var.enable_ethstats~}
  --ethstats "Node${count.index + 1}:${var.ethstats_secret}@${var.ethstats_ip}:${var.ethstats.container.port}" \
%{endif~}
  --unlock ${join(",", range(var.accounts_count[count.index]))} \
  --password ${local.container_geth_datadir}/${var.password_file_name} \
  ${(var.consensus == "istanbul" || var.consensus == "qbft") ? "--istanbul.blockperiod 1 --syncmode full --mine --miner.threads 1" : format("--raft --raftport %d", var.geth_networking[count.index].port.raft)} ${lookup(var.additional_geth_args, count.index, "")} $ADDITIONAL_GETH_ARGS
EOF
  }
}
