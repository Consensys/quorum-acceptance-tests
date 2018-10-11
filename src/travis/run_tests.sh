#!/bin/bash

echo "Running tests"
mvn clean test -q \
  -DTRAVIS=${TRAVIS} \
  -DCONSENSUS=${TF_VAR_consensus_mechanism}