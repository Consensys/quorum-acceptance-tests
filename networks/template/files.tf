resource "local_file" "docker" {
  filename = format("%s/application-docker.yml", module.network.generated_dir)
  content  = <<YML
quorum:
  consensus: ${var.consensus}
  docker-infrastructure:
    enabled: true
    target-quorum-image: ${var.quorum_docker_image.name}
    target-tessera-image: ${var.tessera_docker_image.name}
%{if var.remote_docker_config != null~}
    host: ${var.remote_docker_config.docker_host}
%{endif~}
    nodes:
%{for idx in local.node_indices~}
      Node${idx + 1}:
        quorum-container-id: ${element(module.docker.quorum_containers, idx)}
        tessera-container-id: ${element(module.docker.tessera_containers, idx)}
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
wait.disable = true

spring.profiles.active = ${var.network_name},docker
spring.config.additional-location = file:${module.network.generated_dir}/

EOT
}

resource "local_file" "gauge_env" {
  count    = var.gauge_env_outdir == "" ? 0 : 1
  filename = "${var.gauge_env_outdir}/user.properties"
  content  = <<EOT

# These are environment variables being used by Gauge while running the tests.

SPRING_PROFILES_ACTIVE = ${var.network_name},docker%{if var.remote_docker_config != null~},proxy%{endif}
SPRING_CONFIG_ADDITIONALLOCATION = file:${module.network.generated_dir}/

EOT
}
