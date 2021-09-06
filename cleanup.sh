#!/bin/bash
docker kill $(docker ps -q)
yes | docker system prune
yes | docker volume prune
rm -rf /tmp/run-local
rm -rf /tmp/run-at-besu
rm -rf /tmp/plugins-*
rm -rf /tmp/account-plugin-hashicorp-vault-*
rm -rf /tmp/quorum-plugin-accts-fallback-*rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical/terraform.tfstate.d
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical/.terraform
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical/.terraform.lock.hcl
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical-besu/terraform.tfstate.d
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical-besu/.terraform
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/typical-besu/.terraform.lock.hcl
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/template/terraform.tfstate.d
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/template/.terraform
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/template/.terraform.lock.hcl
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/plugins/.terraform
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/plugins/terraform.tfstate.d
rm -rf /home/baptiste/git/quorum-acceptance-tests/networks/plugins/.terraform.lock.hcl
sudo rm -rf /tmp/acctests