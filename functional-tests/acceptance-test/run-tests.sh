#!/bin/bash
cd ~/quorum-examples/nodejs/acceptance-test/
echo "running acceptance test cases..."
mocha --timeout 80000 *test.js
echo "running acceptance test done."
