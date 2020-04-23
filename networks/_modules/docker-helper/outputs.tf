output "consensus" {
  value = var.consensus
}

output "geth_networking" {
  value = local.geth_networking
}

output "tm_networking" {
  value = local.tm_networking
}

output "geth_docker_config" {
  value = var.geth
}

output "tessera_docker_config" {
  value = var.tessera
}

output "ethstats_secret" {
  value = random_id.ethstats_secret.hex
}

output "ethstat_ip" {
  value = local.ethstats_ip
}

output "network_cidr" {
  value = local.container_network_cidr
}