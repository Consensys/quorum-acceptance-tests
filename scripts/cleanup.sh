#!/bin/bash
docker kill $(docker ps -q)
yes | docker system prune
yes | docker volume prune
rm -rf /tmp/run-local
rm -rf /tmp/run-at-besu
rm -rf /tmp/plugins-*
rm -rf /tmp/account-plugin-hashicorp-vault-*
rm -rf /tmp/quorum-plugin-accts-fallback-*

find . -name terraform.tfstate.d -exec rm -Rf {} \; 
find . -name .terraform -exec rm -Rf {} \; 
find . -name .terraform.lock.hcl -exec rm -Rf {} \; 

rm -rf /tmp/acctests
