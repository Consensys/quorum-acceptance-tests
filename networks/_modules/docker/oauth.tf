locals {
  oauth2_server            = format("%s-oauth2-server", var.network_name)
  oauth2_server_serve_port = { internal = 4444, external = 4444 } # for client to connect and authenticate
  oauth2_server_admin_port = { internal = 4445, external = 4445 } # for admin
  network_config = element(tolist(docker_network.quorum.ipam_config), 0)
  hydra_ip = cidrhost(lookup(local.network_config, "subnet"), 200)
}

resource "docker_container" "hydra" {
  count    = var.oauth2_server.start ? 1 : 0
  image    = "oryd/hydra:v1.3.2-alpine"
  name     = local.oauth2_server
  hostname = local.oauth2_server
  networks_advanced {
    name         = docker_network.quorum.name
    ipv4_address = local.hydra_ip
  }
  env = [
    "URLS_SELF_ISSUER=https://goquorum.com/oauth/",
    "DSN=memory",
    "STRATEGIES_ACCESS_TOKEN=jwt"
  ]
  restart = "unless-stopped"
  ports {
    internal = local.oauth2_server_serve_port.internal
    external = local.oauth2_server_serve_port.external
  }
  ports {
    internal = local.oauth2_server_admin_port.internal
    external = local.oauth2_server_admin_port.external
  }
  healthcheck {
    test         = ["CMD", "nc", "-vz", "localhost", local.oauth2_server_serve_port.internal]
    interval     = "3s"
    retries      = 10
    timeout      = "3s"
    start_period = "5s"
  }
}
