# acceptance test

This is used to test different test scenarios using test scripts based on mocha framework. The test scripts are 
nodejs programs with mocha's `describe/context/it` style test cases.
`web3js` is mainly used to invoke `geth` API calls.

Usage:

- `npm-uninstall.sh`: Remove `npm, node` and all packages 
- `test-init.sh`: Install `node, npm, mocha, tracer, child-process-promise, ethereumjs-tx, keythereum` that are need for testing. And set `NODE_PATH`
- `run-tests.sh`: To run all test cases
- `mocha --timeout 80000 <test-file>`: To run a specific test case. 

##Example test run:


Run a specific test case
```
mocha --timeout 35000 public_smart_contract_test.js
  public contract with emitEvent
    sending from node1
      ✓ should have same number of events/logs visible in all nodes (1326ms)
    sending from node2
      ✓ should have same number of events/logs visible in all nodes (2238ms)
    sending from node3
      ✓ should have same number of events/logs visible in all nodes (1264ms)
    sending from node4
      ✓ should have same number of events/logs visible in all nodes (1269ms)
    sending from node5
      ✓ should have same number of events/logs visible in all nodes (2251ms)
    sending from node6
      ✓ should have same number of events/logs visible in all nodes (1251ms)


  6 passing (10s)

```
To run all test cases:
```
./run-tests.sh
running acceptance test cases...




  config
    accounts
      ✓ should have accounts defined
    nodes
      ✓ should have nodes defined
    constellation
      ✓ should have keys defined

  storage root for private smart contract from node1 to node7
    ✓ should have same store root for a contract in node1 and node7 but different in other nodes (13586ms)

  private signed transaction
    send signed transaction from node1 to other nodes
      ✓ should send signed transaction to other nodes (28673ms)
    send signed transaction from node2 to other nodes
      ✓ should send signed transaction to other nodes (28430ms)
    send signed transaction from node3 to other nodes
      ✓ should send signed transaction to other nodes (27468ms)
    send signed transaction from node4 to other nodes
      ✓ should send signed transaction to other nodes (27412ms)
    send signed transaction from node5 to other nodes
      ✓ should send signed transaction to other nodes (27501ms)
    send signed transaction from node6 to other nodes
      ✓ should send signed transaction to other nodes (27870ms)
    send signed transaction from node7 to other nodes
      ✓ should send signed transaction to other nodes (27462ms)

  private contract with emitEvent
    sending from node1
      ✓ should have same number of events/logs visible in nodes participating in private txn (2246ms)
    sending from node2
      ✓ should have same number of events/logs visible in nodes participating in private txn (2259ms)
    sending from node3
      ✓ should have same number of events/logs visible in nodes participating in private txn (1226ms)
    sending from node4
      ✓ should have same number of events/logs visible in nodes participating in private txn (1214ms)
    sending from node5
      ✓ should have same number of events/logs visible in nodes participating in private txn (1231ms)
    sending from node6
      ✓ should have same number of events/logs visible in nodes participating in private txn (2206ms)

  PrivateSendTransaction in parallel
    ✓ should run in parallel across node1 to node7 (5726ms)

  PrivateSendTransaction with ether value
    ✓ should fail

  PrivateSendTransaction in sequence
    sendTransaction
      ✓ should work from node1 (4128ms)
    sendTransaction
      ✓ should work from node2 (3144ms)
    sendTransaction
      ✓ should work from node3 (4134ms)
    sendTransaction
      ✓ should work from node4 (2150ms)
    sendTransaction
      ✓ should work from node5 (3135ms)
    sendTransaction
      ✓ should work from node6 (3127ms)
    sendTransaction
      ✓ should work from node7 (4133ms)

  signed transaction
    send signed transaction from node1 to other nodes
      ✓ should send signed transaction to other nodes (27449ms)
    send signed transaction from node2 to other nodes
      ✓ should send signed transaction to other nodes (28971ms)
    send signed transaction from node3 to other nodes
      ✓ should send signed transaction to other nodes (28864ms)
    send signed transaction from node4 to other nodes
      ✓ should send signed transaction to other nodes (27912ms)
    send signed transaction from node5 to other nodes
      ✓ should send signed transaction to other nodes (29677ms)
    send signed transaction from node6 to other nodes
      ✓ should send signed transaction to other nodes (27177ms)
    send signed transaction from node7 to other nodes
      ✓ should send signed transaction to other nodes (28420ms)

  public contract with emitEvent
    sending from node1
      ✓ should have same number of events/logs visible in all nodes (1232ms)
    sending from node2
      ✓ should have same number of events/logs visible in all nodes (1193ms)
    sending from node3
      ✓ should have same number of events/logs visible in all nodes (1243ms)
    sending from node4
      ✓ should have same number of events/logs visible in all nodes (1172ms)
    sending from node5
      ✓ should have same number of events/logs visible in all nodes (2217ms)
    sending from node6
      ✓ should have same number of events/logs visible in all nodes (1159ms)

  PublicSendTransaction in parallel
    ✓ should run in parallel across node1 to node7 (4418ms)

  PublicSendTransaction in sequence
    sendTransaction
      ✓ should work from node1 (3113ms)
    sendTransaction
      ✓ should work from node2 (4106ms)
    sendTransaction
      ✓ should work from node3 (3123ms)
    sendTransaction
      ✓ should work from node4 (3107ms)
    sendTransaction
      ✓ should work from node5 (4110ms)
    sendTransaction
      ✓ should work from node6 (5120ms)
    sendTransaction
      ✓ should work from node7 (4119ms)


  47 passing (8m)

vagrant@ubuntu-xenial:~/quorum-examples/nodejs/acceptance-test$
vagrant@ubuntu-xenial:~/quorum-examples/nodejs/acceptance-test$
vagrant@ubuntu-xenial:~/quorum-examples/nodejs/acceptance-test$
vagrant@ubuntu-xenial:~/quorum-examples/nodejs/acceptance-test$
vagrant@ubuntu-xenial:~/quorum-examples/nodejs/acceptance-test$ mocha --timeout 80000 *test.js


  config
    accounts
      ✓ should have accounts defined
    nodes
      ✓ should have nodes defined
    constellation
      ✓ should have keys defined

  storage root for private smart contract from node1 to node7
    ✓ should have same store root for a contract in node1 and node7 but different in other nodes (13620ms)

  private signed transaction
    send signed transaction from node1 to other nodes
      ✓ should send signed transaction to other nodes (29857ms)
    send signed transaction from node2 to other nodes
      ✓ should send signed transaction to other nodes (28634ms)
    send signed transaction from node3 to other nodes
      ✓ should send signed transaction to other nodes (27840ms)
    send signed transaction from node4 to other nodes
      ✓ should send signed transaction to other nodes (29708ms)
    send signed transaction from node5 to other nodes
      ✓ should send signed transaction to other nodes (28682ms)
    send signed transaction from node6 to other nodes
      ✓ should send signed transaction to other nodes (26819ms)
    send signed transaction from node7 to other nodes
      ✓ should send signed transaction to other nodes (28267ms)

  private contract with emitEvent
    sending from node1
      ✓ should have same number of events/logs visible in nodes participating in private txn (2296ms)
    sending from node2
      ✓ should have same number of events/logs visible in nodes participating in private txn (1253ms)
    sending from node3
      ✓ should have same number of events/logs visible in nodes participating in private txn (2289ms)
    sending from node4
      ✓ should have same number of events/logs visible in nodes participating in private txn (1244ms)
    sending from node5
      ✓ should have same number of events/logs visible in nodes participating in private txn (2259ms)
    sending from node6
      ✓ should have same number of events/logs visible in nodes participating in private txn (2259ms)

  PrivateSendTransaction in parallel
    ✓ should run in parallel across node1 to node7 (5753ms)

  PrivateSendTransaction with ether value
    ✓ should fail

  PrivateSendTransaction in sequence
    sendTransaction
      ✓ should work from node1 (2167ms)
    sendTransaction
      ✓ should work from node2 (4145ms)
    sendTransaction
      ✓ should work from node3 (4169ms)
    sendTransaction
      ✓ should work from node4 (3137ms)
    sendTransaction
      ✓ should work from node5 (3162ms)
    sendTransaction
      ✓ should work from node6 (5141ms)
    sendTransaction
      ✓ should work from node7 (4142ms)

  signed transaction
    send signed transaction from node1 to other nodes
      ✓ should send signed transaction to other nodes (28575ms)
    send signed transaction from node2 to other nodes
      ✓ should send signed transaction to other nodes (28382ms)
    send signed transaction from node3 to other nodes
      ✓ should send signed transaction to other nodes (29692ms)
    send signed transaction from node4 to other nodes
      ✓ should send signed transaction to other nodes (29153ms)
    send signed transaction from node5 to other nodes
      ✓ should send signed transaction to other nodes (28451ms)
    send signed transaction from node6 to other nodes
      ✓ should send signed transaction to other nodes (29346ms)
    send signed transaction from node7 to other nodes
      ✓ should send signed transaction to other nodes (29694ms)

  public contract with emitEvent
    sending from node1
      ✓ should have same number of events/logs visible in all nodes (1324ms)
    sending from node2
      ✓ should have same number of events/logs visible in all nodes (2192ms)
    sending from node3
      ✓ should have same number of events/logs visible in all nodes (248ms)
    sending from node4
      ✓ should have same number of events/logs visible in all nodes (2268ms)
    sending from node5
      ✓ should have same number of events/logs visible in all nodes (1189ms)
    sending from node6
      ✓ should have same number of events/logs visible in all nodes (1135ms)

  PublicSendTransaction in parallel
    ✓ should run in parallel across node1 to node7 (6406ms)

  PublicSendTransaction in sequence
    sendTransaction
      ✓ should work from node1 (3129ms)
    sendTransaction
      ✓ should work from node2 (3123ms)
    sendTransaction
      ✓ should work from node3 (3154ms)
    sendTransaction
      ✓ should work from node4 (4142ms)
    sendTransaction
      ✓ should work from node5 (3121ms)
    sendTransaction
      ✓ should work from node6 (3114ms)
    sendTransaction
      ✓ should work from node7 (3114ms)


  47 passing (8m)
```      

