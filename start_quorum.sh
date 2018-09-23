#!/bin/bash

set -e

TERRAFORM_CMD=${TERRAFORM_CMD:-terraform}

echo "Provisioning Quorum Network"
pushd ${QUORUM_CLOUD_TEMPLATES_DIR}/_terraform_init > /dev/null
${TERRAFORM_CMD} init
${TERRAFORM_CMD} apply -var network_name=ci-${TF_VAR_consensus_mechanism}-${TRAVIS_COMMIT::6} -auto-approve
popd > /dev/null

pushd ${QUORUM_CLOUD_TEMPLATES_DIR} > /dev/null
${TERRAFORM_CMD} init -no-color -backend-config=terraform.auto.backend_config
${TERRAFORM_CMD} apply -auto-approve
export private_key_file=$(${TERRAFORM_CMD} output -json | jq .private_key_file.value)
echo ${private_key_file}
export bastion_host_ip=$(${TERRAFORM_CMD} output -json | jq .bastion_host_ip.value)
echo ${bastion_host_ip}
popd > /dev/null

echo "Wait for the Quorum Network being ready"
while [ ! -f "config/application-local.yml" ]; do
    sleep 3
    scp -o ServerAliveInterval=30 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=quiet \
        -i ${private_key_file} ec2-user@${bastion_host_ip}:/qdata/quorum_metadata config/application-local.yml > /dev/null 2>&1
done

echo "Start SOCKS proxy for SSH tunnelling"
ssh -D 5000 -N -o ServerAliveInterval=30 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=quiet \
      -i ${private_key_file} ec2-user@${bastion_host_ip} &