resource "local_file" "docker" {
  filename = format("%s/application-docker.yml", module.network.generated_dir)
  content  = <<YML
quorum:
  consensus: ${var.consensus}
  docker-infrastructure:
    enabled: true
    target-quorum-image: ${var.geth.container.image.name}
    target-tessera-image: ${var.tessera.container.image.name}
    target-besu-image: ${var.besu.container.image.name}
    target-ethsigner-image: ${var.ethsigner.container.image.name}
%{if var.remote_docker_config != null~}
    host: ${var.remote_docker_config.docker_host}
%{endif~}
    nodes:
%{for idx in local.quorum_node_indices~}
      Node${idx + 1}:
        quorum-container-id: ${element(module.docker.quorum_containers, idx)}
        tessera-container-id: ${element(module.docker.tessera_containers, idx)}
%{endfor~}
%{for idx in local.besu_node_indices~}
      Node${idx + local.number_of_quorum_nodes + 1}:
        quorum-container-id: ${element(module.docker-besu.besu_containers, idx)}
        tessera-container-id: ${element(module.docker-besu.tessera_containers, idx)}
        ethsigner-container-id: ${element(module.docker-besu.ethsigner_containers, idx)}
%{endfor~}
YML
}

resource "local_file" "proxy" {
  count    = var.remote_docker_config == null ? 0 : 1
  filename = format("%s/application-proxy.yml", module.network.generated_dir)
  content  = <<YML
quorum:
  socks-proxy:
    tunnel:
      enabled: true
      auto-start: true
      user: ${var.remote_docker_config.ssh_user}
      host: ${var.remote_docker_config.ssh_host}
      private-key-file: "${var.remote_docker_config.private_key_file}"
YML
}

resource "local_file" "dockerwaitmain" {
  count    = var.properties_outdir == "" ? 0 : 1
  filename = "${var.properties_outdir}/DockerWaitMain-network.properties"
  content  = <<EOT

# These are properties being used by DockerWait Spring Boot application.
# 'docker' profile is activated in order to allow the application perform health checking on the containers.

# template network we don't need to do health check
%{if var.template_network == true~}
wait.disable = true
%{endif~}

spring.profiles.active = ${var.network_name},docker
spring.config.additional-location = file:${module.network.generated_dir}/

EOT
}

resource "local_file" "gauge_env" {
  count    = var.gauge_env_outdir == "" ? 0 : 1
  filename = "${var.gauge_env_outdir}/user.properties"
  content  = <<EOT

# These are environment variables being used by Gauge while running the tests.
%{if var.template_network == true~}
SPRING_PROFILES_ACTIVE = ${var.network_name},docker%{if var.remote_docker_config != null~},proxy%{endif}
%{else~}
SPRING_PROFILES_ACTIVE = ${var.network_name}%{if var.remote_docker_config != null~},proxy%{endif}
%{endif~}
SPRING_CONFIG_ADDITIONALLOCATION = file:${module.network.generated_dir}/

EOT
}
