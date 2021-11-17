provider "aws" {
  region = terraform.workspace
}

locals {
  docker_tcp_port = 2376
  run_id          = random_id.run_id.hex
  common_tags = {
    By    = data.aws_caller_identity.this.user_id
    For   = "quorum-acceptance-tests"
    Using = "Terraform"
    RunId = local.run_id
    RunOn = var.run_on
    Name  = "acctests-${local.run_id}"
  }
  ssh_key_name = "ephemeral-${local.run_id}"
  user         = "ec2-user"
}

data "aws_caller_identity" "this" {
}

resource "random_id" "run_id" {
  byte_length = 8
}

// using AMAZON Linux 2 AMI
// login using `ec2-user`
data "aws_ami" "this" {
  most_recent = true
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
  owners = ["137112412989"] # amazon
}

data "http" "myip" {
  url = "https://ifconfig.co/"
}

resource "tls_private_key" "ssh" {
  algorithm = "RSA"
  rsa_bits  = "4096"
}

resource "aws_key_pair" "ssh" {
  public_key      = tls_private_key.ssh.public_key_openssh
  key_name_prefix = "${local.ssh_key_name}-"
  tags            = local.common_tags
}

resource "local_file" "private_key" {
  filename        = "${path.module}/${local.ssh_key_name}.pem"
  content         = tls_private_key.ssh.private_key_pem
  file_permission = "600"
}

resource "aws_security_group" "this" {
  vpc_id = var.vpc_id
  ingress {
    from_port   = 22
    protocol    = "tcp"
    to_port     = 22
    cidr_blocks = ["${chomp(data.http.myip.body)}/32"]
    description = "SSH"
  }
  ingress {
    from_port   = local.docker_tcp_port
    protocol    = "tcp"
    to_port     = local.docker_tcp_port
    cidr_blocks = ["${chomp(data.http.myip.body)}/32"]
    description = "Docker Remote"
  }
  egress {
    from_port   = 0
    protocol    = -1
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.common_tags
}

resource "aws_instance" "my" {
  ami                         = data.aws_ami.this.id
  instance_type               = "c5.2xlarge"
  key_name                    = aws_key_pair.ssh.key_name
  associate_public_ip_address = true
  vpc_security_group_ids      = [aws_security_group.this.id]
  subnet_id                   = var.public_subnet_id
  user_data                   = <<EOF
#!/bin/bash

set -e

# START: added per suggestion from AWS support to mitigate an intermittent failures from yum update
sleep 20
yum clean all
yum repolist
# END

yum -y update
amazon-linux-extras install docker -y

systemctl enable docker
systemctl start docker
mkdir -p /etc/systemd/system/docker.service.d
cat <<CONF > /etc/systemd/system/docker.service.d/override.conf
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:${local.docker_tcp_port}
CONF
systemctl daemon-reload
systemctl restart docker

usermod -a -G docker ${local.user}

EOF

  tags = local.common_tags
}

resource "local_file" "tfvars" {
  count    = var.tfvars_outdir == "" ? 0 : 1
  filename = "${var.tfvars_outdir}/terraform.auto.tfvars"
  content  = <<EOT
# These are used by networks to connect to the VM

remote_docker_config = {
  ssh_user = "${local.user}"
  ssh_host = "${local.host}"
  private_key_file = "${local.private_key_file}"
  docker_host = "${local.docker_host}"
}
EOT
}

resource "local_file" "dockerwaitmain" {
  count    = var.properties_outdir == "" ? 0 : 1
  filename = "${var.properties_outdir}/DockerWaitMain-infra.properties"
  content  = <<EOP

# These properties are used by DockerWaitMain to hold until infra is ready
# This is auto generated file. Please do not edit
wait.enabled = true

# The below properties set the QuorumNetworkProperty data
quorum.docker-infrastructure.enabled = true
quorum.docker-infrastructure.host = ${local.docker_host}

EOP
}