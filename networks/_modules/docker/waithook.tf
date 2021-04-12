# container that does the work of waithook.com which is very unstable causing our tests to fail
resource "docker_container" "waithook" {
  depends_on = [docker_container.ethstats]
  image      = "erolagnab/waithook:latest"
  name       = format("%s-waithook", var.network_name)
  hostname   = "waithook.local"
  restart    = "no"
  networks_advanced {
    name    = docker_network.quorum.name
    aliases = ["waithook.local"]
  }
  ports {
    internal = 3012
    external = 3012
  }
  healthcheck {
    test         = ["CMD", "nc", "-vz", "localhost", "3012"]
    interval     = "3s"
    retries      = 10
    timeout      = "3s"
    start_period = "3s"
  }
}