output "generated_dir" {
  value = module.network.generated_dir
}

output "properties" {
  description = <<EOT
These are properties being used by DockerWait Spring Boot application.
'docker' profile is activated in order to allow the application perform health checking on the containers.
EOT
  value = <<TXT

spring.profiles.active = ${var.network_name},docker
spring.config.additional-location = file:${module.network.generated_dir}/

TXT
}

output "environments" {
  description = <<EOT
These are environment variables being used by Gauge while running the tests.
We don't need 'docker' spring profile here as typical tests don't manipulate network.
EOT
  value = <<TXT

SPRING_PROFILES_ACTIVE = ${var.network_name}
SPRING_CONFIG_ADDITIONALLOCATION = file:${module.network.generated_dir}/

TXT
}