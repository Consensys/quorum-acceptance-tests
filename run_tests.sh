#!/bin/bash

export QUORUM_SOCKS_PROXY_HOST=localhost
export QUORUM_SOCKS_PROXY_PORT=5000
echo "Running tests"
mvn clean test -q