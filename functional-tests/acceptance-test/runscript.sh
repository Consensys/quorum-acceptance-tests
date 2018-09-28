#!/bin/bash
#PRIVATE_CONFIG=qdata/c1/tm.ipc geth --exec "loadScript(\"$1\")" attach ipc:qdata/dd1/geth.ipc
PRIVATE_CONFIG=/home/vagrant/quorum-examples/examples/7nodes/qdata/c1/tm.ipc geth --exec "loadScript(\"$1\")" attach ipc:/home/vagrant/quorum-examples/examples/7nodes/qdata/dd1/geth.ipc
