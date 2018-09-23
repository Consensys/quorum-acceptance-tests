#!/bin/bash

echo "Destroying Quorum Network"
pushd ${QUORUM_CLOUD_TEMPLATES_DIR} > /dev/null
${TERRAFORM_CMD} destroy -auto-approve
popd > /dev/null