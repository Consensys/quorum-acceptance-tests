resource "docker_container" "tessera" {
  count             = local.number_of_nodes
  name              = format("%s-tm%d", var.network_name, count.index)
  image             = docker_image.tessera.name
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
    internal = var.tessera.container.port.p2p
  }
  ports {
    internal = var.tessera.container.port.thirdparty
    external = var.tessera.host.port.thirdparty_start + count.index
  }
  volumes {
    container_path = "/data"
    volume_name    = docker_volume.shared_volume[count.index].name
  }
  volumes {
    container_path = local.container_tm_datadir_mounted
    host_path      = var.tessera_datadirs[count.index]
  }
  networks_advanced {
    name         = docker_network.quorum.name
    ipv4_address = var.tm_networking[count.index].ip.private
    aliases      = [format("tm%d", count.index)]
  }
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
#Tessera${count.index + 1}

if [ "$ALWAYS_REFRESH" == "true" ]; then
  echo "Deleting ${local.container_tm_datadir} to refresh with original datadir"
  rm -rf ${local.container_tm_datadir}
fi
if [ ! -d "${local.container_tm_datadir}" ]; then
  echo "Copying mounted datadir to ${local.container_tm_datadir}"
  cp -r ${local.container_tm_datadir_mounted} ${local.container_tm_datadir}
fi
rm -f ${local.container_tm_ipc_file}
exec java -Xms128M -Xmx128M \
  -jar /tessera/tessera-app.jar \
  --override jdbc.url="jdbc:h2:${local.container_tm_datadir}/db;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=0" \
  --override serverConfigs[1].serverAddress="unix:${local.container_tm_ipc_file}" \
  --override serverConfigs[2].sslConfig.serverKeyStore="${local.container_tm_datadir}/serverKeyStore" \
  --override serverConfigs[2].sslConfig.serverTrustStore="${local.container_tm_datadir}/serverTrustStore" \
  --override serverConfigs[2].sslConfig.knownClientsFile="${local.container_tm_datadir}/knownClientsFile" \
  --override serverConfigs[2].sslConfig.clientKeyStore="${local.container_tm_datadir}/clientKeyStore" \
  --override serverConfigs[2].sslConfig.clientTrustStore="${local.container_tm_datadir}/clientTrustStore" \
  --override serverConfigs[2].sslConfig.knownServersFile="${local.container_tm_datadir}/knownServersFile" \
  --configfile ${local.container_tm_datadir}/config.json
EOF
  ]
}