resource "docker_container" "tessera" {
  count             = local.number_of_nodes
  name              = format("%s-tm%d", var.network_name, count.index)
  depends_on        = [docker_image.local, docker_image.registry]
  image             = var.tm_networking[count.index].image.name
  hostname          = format("tm%d", count.index)
  restart           = "no"
  publish_all_ports = false
  must_run          = var.start_tessera
  start             = var.start_tessera
  labels {
    label = "TesseraContainer"
    value = count.index
  }
  ports {
    internal = var.tm_networking[count.index].port.p2p
  }
  ports {
    internal = var.tm_networking[count.index].port.thirdparty.internal
    external = var.tm_networking[count.index].port.thirdparty.external
  }
  volumes {
    container_path = "/data"
    volume_name    = docker_volume.shared_volume[count.index].name
  }
  volumes {
    container_path = local.container_tm_datadir_mounted
    host_path      = var.tessera_datadirs[count.index]
  }
  dynamic "volumes" {
    for_each = lookup(var.additional_tessera_container_vol, count.index, [])
    content {
      container_path = volumes.value["container_path"]
      host_path      = volumes.value["host_path"]
    }
  }
  networks_advanced {
    name         = docker_network.quorum.name
    ipv4_address = var.tm_networking[count.index].ip.private
    aliases      = [format("tm%d", count.index)]
  }
  env = local.tm_env
  healthcheck {
    test         = ["CMD-SHELL", "[ -S ${local.container_tm_ipc_file} ] || exit 1"]
    interval     = "3s"
    retries      = 20
    timeout      = "3s"
    start_period = "5s"
  }
  entrypoint = [
    "/bin/sh",
    "-c",
    <<EOF
echo "Tessera GoQuorum ${count.index + 1}"

JAVA_OPTS="-Xms128M -Xmx128M"
RUN_COMMAND="/tessera/bin/tessera"

// TODO: remove following block when Jigsaw dist is default
JAR_FILE="${lookup(var.tessera_app_container_path, count.index, "/tessera/tessera-app.jar")}"

if [[ -f "$${JAR_FILE}" ]];
then
  RUN_COMMAND="java -Xms128M -Xmx128M -jar $${JAR_FILE}"
fi
// end of block to remove

START_TESSERA="$${RUN_COMMAND} \
  --override jdbc.url=jdbc:h2:${local.container_tm_datadir}/db;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=0 \
  --override serverConfigs[1].serverAddress=unix:${local.container_tm_ipc_file} \
  --override serverConfigs[2].sslConfig.serverKeyStore=${local.container_tm_datadir}/serverKeyStore \
  --override serverConfigs[2].sslConfig.serverTrustStore=${local.container_tm_datadir}/serverTrustStore \
  --override serverConfigs[2].sslConfig.knownClientsFile=${local.container_tm_datadir}/knownClientsFile \
  --override serverConfigs[2].sslConfig.clientKeyStore=${local.container_tm_datadir}/clientKeyStore \
  --override serverConfigs[2].sslConfig.clientTrustStore=${local.container_tm_datadir}/clientTrustStore \
  --override serverConfigs[2].sslConfig.knownServersFile=${local.container_tm_datadir}/knownServersFile \
  --configfile ${local.container_tm_datadir}/config.json"

echo $START_TESSERA

if [ "$ALWAYS_REFRESH" == "true" ]; then
  echo "Deleting ${local.container_tm_datadir} to refresh with original datadir"
  rm -rf ${local.container_tm_datadir}
fi
if [ ! -d "${local.container_tm_datadir}" ]; then
  echo "Copying mounted datadir to ${local.container_tm_datadir}"
  cp -r ${local.container_tm_datadir_mounted} ${local.container_tm_datadir}
fi

if [ -f /data/tm/cleanStorage ]; then
  echo "Cleaning tessera storage."
  rm -rf /data/tm/db*
  echo "Starting tessera resend."
  $START_TESSERA -r
  status=$?
  echo "Tessera resend result: $status"
  rm /data/tm/cleanStorage
  if [ $status -eq 0 ]; then
    echo "Tessera resend successful."
  else
    echo "Tessera resend failed."
    exit 1
  fi
fi

rm -f ${local.container_tm_ipc_file}
exec $START_TESSERA
EOF
  ]
}
