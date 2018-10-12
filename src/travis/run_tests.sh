#!/bin/bash

echo "Running tests"
ENV_DIR="travis-build-${TRAVIS_JOB_NUMBER}"
mkdir -p env/${ENV_DIR}
mvn clean test -q \
  -DTRAVIS=${TRAVIS} \
  -DCONSENSUS=${TF_VAR_consensus_mechanism} \
  -Denv=${ENV_DIR}