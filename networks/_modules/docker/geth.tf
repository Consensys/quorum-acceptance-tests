locals {
  publish_http_ports = [for idx in local.node_indices : [var.geth_networking[idx].port.http]]
  publish_ws_ports = var.geth_networking[0].port.ws == null ? [for idx in local.node_indices : []] : [for idx in local.node_indices : [var.geth_networking[idx].port.ws]]

  internal_node_rpc_urls = [for idx in local.node_indices : format("http://%s:%d", var.geth_networking[idx].ip.private, var.geth_networking[idx].port.http.internal)]
}

resource "docker_container" "geth" {
  count      = local.number_of_nodes
  name       = format("%s-node%d", var.network_name, count.index)
  depends_on = [docker_container.ethstats, docker_image.registry, docker_image.local, docker_container.hydra]
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
    internal = var.geth_networking[count.index].port.qlight
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
  env = contains(local.qlight_client_indices, count.index) ? local.qlight_client_env : local.geth_env
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

if [ ! -f "${local.container_geth_datadir}/legacy-genesis.json" ] || [! -f "${local.container_geth_datadir}/latest-genesis.json"]; then
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

if ${contains(local.non_qlight_client_node_indices, count.index)}; then
  echo "waiting for tessera"
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
else
  echo "not waiting for tessera"
fi

EOF
  }

  upload {
    file       = "${local.container_geth_datadir_mounted}/wait-for-oauth2-server.sh"
    executable = true
    content    = <<EOF
#!/bin/sh

URL="https://${local.hydra_ip}:${var.oauth2_server.admin_port.internal}/clients"
echo "waiting for oauth2 server on $URL"

UDS_WAIT=10
for i in $(seq 1 100)
do
  result=$(wget --timeout $UDS_WAIT -qO- --proxy off --no-check-certificate $URL)
  code=$?
  echo "resp=$result"
  if [ "$code" == "0" ] ; then
    echo "oauth2 server appears to be up"
    break
  else
    echo "Sleep $UDS_WAIT seconds. Waiting for oauth2 server."
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

if [ $VERSION == '2.5.0' ] || [ $VERSION == '21.10.0' ] || [ $VERSION == '21.4.0' ] || [ $VERSION == '22.1.1' ]; then
  echo "Initializing geth with legacy genesis"
  cat ${local.container_geth_datadir}/legacy-genesis.json
  geth --datadir ${local.container_geth_datadir} init ${local.container_geth_datadir}/legacy-genesis.json
else
  echo "Initializing geth with latest genesis"
  cat ${local.container_geth_datadir}/latest-genesis.json
  geth --datadir ${local.container_geth_datadir} init ${local.container_geth_datadir}/latest-genesis.json
fi

#exit if geth init fails
rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi



if [[ $VERSION == '2.5.0' ]]; then
  HTTP_ARGS="--rpc \
  --rpcaddr 0.0.0.0 \
  --rpcport ${var.geth_networking[count.index].port.http.internal} \
  --rpcapi admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,quorumPermission,quorumExtension,${(var.consensus == "istanbul" || var.consensus == "qbft" ? "istanbul" : "raft")} "

  export ADDITIONAL_GETH_ARGS="${lookup(var.additional_geth_args, count.index, "")} $ADDITIONAL_GETH_ARGS" | sed 's/--allow-insecure-unlock//g'
else
  HTTP_ARGS="--http \
  --http.addr 0.0.0.0 \
  --http.port ${var.geth_networking[count.index].port.http.internal} \
  --http.api admin,db,eth,debug,miner,net,shh,txpool,personal,web3,quorum,quorumPermission,quorumExtension,${(var.consensus == "istanbul" || var.consensus == "qbft" ? "istanbul" : "raft")} "

  export ADDITIONAL_GETH_ARGS="${lookup(var.additional_geth_args, count.index, "")} $ADDITIONAL_GETH_ARGS"
fi

if [ $VERSION == '2.5.0' ] || [ $VERSION == '21.10.0' ] || [ $VERSION == '22.1.1' ]; then
  export ADDITIONAL_GETH_ARGS="${(var.consensus == "istanbul" || var.consensus == "qbft") ? "--istanbul.blockperiod 1 " : ""} $ADDITIONAL_GETH_ARGS"
fi

%{if contains(var.qlight_server_indices, count.index)~}
echo "qlight server"
QLIGHT_ARGS="--qlight.server \
  --qlight.server.p2p.port ${var.geth_networking[count.index].port.qlight}"
%{endif}

%{if lookup(var.qlight_clients, tostring(count.index), null) != null~}
echo "qlight client"
QLIGHT_ARGS="--qlight.client \
  --qlight.client.serverNode ${var.qlight_p2p_urls[var.qlight_clients[tostring(count.index)].server_idx]} \
%{if var.qlight_clients[tostring(count.index)].mps_psi != ""~}
  --qlight.client.psi ${var.qlight_clients[tostring(count.index)].mps_psi} \
%{endif~}
%{if var.qlight_clients[tostring(count.index)].mt_is_server_tls_enabled~}
  --qlight.client.serverNodeRPC ${replace(local.internal_node_rpc_urls[var.qlight_clients[tostring(count.index)].server_idx], "http", "https")}"
%{else~}
  --qlight.client.serverNodeRPC ${local.internal_node_rpc_urls[var.qlight_clients[tostring(count.index)].server_idx]}"
%{endif~}

# we know this node is a qlight client - if its qlight server is a multitenant node, then get the required oauth2 token and add the corresponding CLI flags
%{if length(regexall("^.*--multitenancy.*$", lookup(var.additional_geth_args, var.qlight_clients[tostring(count.index)].server_idx, ""))) > 0}
apk add jq

OAUTH_PSI="${var.qlight_clients[tostring(count.index)].mps_psi}"
OAUTH_AUDIENCE="Node${var.qlight_clients[tostring(count.index)].server_idx + 1}"
OAUTH_SCOPE="${var.qlight_clients[tostring(count.index)].mt_scope}"

echo "getting oauth2 token $OAUTH_PSI $OAUTH_AUDIENCE $OAUTH_SCOPE"
echo "waiting for oauth2 server"
${local.container_geth_datadir_mounted}/wait-for-oauth2-server.sh

curl -k -q -X DELETE \
    https://${local.hydra_ip}:${var.oauth2_server.admin_port.internal}/clients/$OAUTH_PSI

curl -k -s -X POST \
    -H "Content-Type: application/json" \
    --data "{\"grant_types\":[\"client_credentials\"],\"token_endpoint_auth_method\":\"client_secret_post\",\"audience\":[\"$OAUTH_AUDIENCE\"],\"client_id\":\"$OAUTH_PSI\",\"client_secret\":\"foofoo\",\"scope\":\"$OAUTH_SCOPE\"}" \
    https://${local.hydra_ip}:${var.oauth2_server.admin_port.internal}/clients | jq -r

post_resp=$(curl -k -s -X POST \
    -F "grant_type=client_credentials" \
    -F "client_id=$OAUTH_PSI" \
    -F "client_secret=foofoo" \
    -F "scope=$OAUTH_SCOPE" \
    -F "audience=$OAUTH_AUDIENCE" \
    https://${local.hydra_ip}:${var.oauth2_server.serve_port.internal}/oauth2/token)

access_token="$(echo $post_resp | jq '.access_token' -r)"

# the --qlight.client.token.value flag is applied later and not as part of QLIGHT_ARGS.  This was required to
# handle the behaviour/limitations of the POSIX /bin/sh.  The flag value is of the form "bearer mytoken".  The space
# causes problems when passed to exec as part of the string ARGS (i.e. exec geth $ARGS).  The POSIX shell splits the
# ARGS string on all spaces, ignoring the surrounding quotes, resulting in the incorrect interpretation of the token
# flag value and erroring the exec.  exec geth "$ARGS" causes geth to parse the entire ARGS as a single
# arg which is obviously also incorrect.  No combination of quoting/escaping could be found to resolve this.  One
# possible solution would be to use an array for ARGS instead of a string but arrays are not supported in the POSIX
# shell.  So, the only solution found so far has been to add the --qlight.client.token.value flag directly to the cmd
# and not as part of the QLIGHT_ARGS string variable.
QLIGHT_ARGS="$QLIGHT_ARGS \
  --qlight.client.token.enabled \
  --qlight.client.token.management none"
%{endif}
%{endif}
echo "QLIGHT_ARGS=$QLIGHT_ARGS"

ARGS="--identity Node${count.index + 1} \
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
%{if !contains(local.qlight_client_indices, count.index)~}
  --unlock ${join(",", range(var.accounts_count[count.index]))} \
  --password ${local.container_geth_datadir}/${var.password_file_name} \
%{endif}
  --syncmode full \
%{if count.index==0~}
  --gcmode archive \
%{endif~}
%{if contains(local.non_qlight_client_node_indices, count.index)~}
%{if var.consensus == "raft"~}
  ${format("--raft --raftport %d", var.geth_networking[count.index].port.raft)} \
%{else~}
  --mine --miner.threads 1 \
%{endif~}
%{endif~}
  $ADDITIONAL_GETH_ARGS \
  $QLIGHT_ARGS"

if [ "$access_token" != "" ]; then
  # see the explanation earlier for why --qlight.client.token.value has to be defined here and not in ARGS
  set -x
  exec geth $ARGS --qlight.client.token.value "bearer $access_token"
  set +x
else
  set -x
  exec geth $ARGS
  set +x
fi

EOF
  }
}
