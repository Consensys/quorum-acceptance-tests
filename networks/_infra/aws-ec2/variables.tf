variable "vpc_id" {}
variable "public_subnet_id" {}
variable "run_on" {
  default = "local"
}
variable "tfvars_outdir" {
  default     = ""
  description = "Output directory containing terraform.auto.tfvars"
}
variable "properties_outdir" {
  default     = ""
  description = "Output directory containing DockerWaitMain.properties"
}