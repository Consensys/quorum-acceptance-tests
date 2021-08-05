# Istanbul fault tolerance and recoverability

 Tags: basic-istanbul, recoverability

[Istanbul consensus](https://github.com/ethereum/EIPs/issues/650) can tolerate at most of F faulty nodes in a N validator nodes network, where N = 3F + 1.

This test is to validate the above feature. Assume that our network has 4 nodes which are all validators, then it will first stop 1 validator, the network should still progress, stop another validator and the network should stop progressing
## Stop F validators followed by F+1 validators

* The consensus should work at the beginning
* Among all validators, stop F validators
* The consensus should work after stopping F validators
* Resume the stopped validators
* Among all validators, stop F+1 validators
* The consensus should stop
* Resume the stopped validators
* The consensus should work after resuming
* Deploy `ClientReceipt` smart contract from a default account in "Node1", named this contract as "contract12"
* "contract12" is mined
