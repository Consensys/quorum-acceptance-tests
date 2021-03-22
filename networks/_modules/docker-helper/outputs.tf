output "consensus" {
  value = var.consensus
}

output "geth_networking" {
  value = local.geth_networking
}

output "tm_networking" {
  value = local.tm_networking
}

output "besu_networking" {
  value = local.besu_networking
}

output "ethsigner_networking" {
  value = local.ethsigner_networking
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
