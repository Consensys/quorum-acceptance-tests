# Istanbul BFT consensus - Validator and non-validator behaviour

  Tags: networks/typical-hybrid::hybrid-q1b2, pre-condition/no-record-blocknumber, hybrid-validator-management

This specification describes how validator and non-validator nodes behave in hybrid network
 - validator node can seal new blocks
 - non-validator node is not authorized to seal new blocks but can sync up

* Start a Quorum Network, named it "mynetwork", consisting of "Node1,Node3,Node4"
* Send some transactions to create blocks in network "mynetwork" and capture the latest block height as "latestBlockHeight"
* Add "Node2" and join the network "mynetwork" as "nonvalidator"

## A new node can sync up with the network but is not allowed to seal blocks

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup, non-validator

New node after being added to the network as non-validator node must not be able to seal new blocks

* Deploy a simple smart contract from "Node2", verify it gets mined
* Deploy a simple smart contract from "Node1", verify it gets mined
* Record the current block number, named it as "blockHeightAfterContractsAreMinted"
* Wait for node "Node2" to catch up to "blockHeightAfterContractsAreMinted"
* Verify node "Node2" has the block height greater or equal to "blockHeightAfterContractsAreMinted"
* "Node2" is not able to seal new blocks

## A new node is allowed to seal blocks after being proposed as a validator

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup, propose-validator

New node after being added to the network as non-valiator node, it's then proposed by other nodes to be validator node.
Hence it is authorized to seal new blocks

* Propose "Node2" to become validator by "Node1,Node3,Node4"
* Deploy a simple smart contract from "Node2", verify it gets mined
* Deploy a simple smart contract from "Node1", verify it gets mined
* Record the current block number, named it as "blockHeightAfterContractsAreMinted"
* Wait for node "Node2" to catch up to "blockHeightAfterContractsAreMinted"
* Verify node "Node2" has the block height greater or equal to "blockHeightAfterContractsAreMinted"
* "Node2" is able to seal new blocks


## A node can still sync up with the network but is not allowed to seal blocks after being proposed as a non-validator

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup, propose-non-validator

Nodes in a network can send proposal to remove a node from validator set

* Propose "Node2" to become validator by "Node1,Node3,Node4"
* Propose "Node1" to become non validator by "Node2,Node3,Node4"
* Deploy a simple smart contract from "Node2", verify it gets mined
* Deploy a simple smart contract from "Node1", verify it gets mined
* Record the current block number, named it as "blockHeightAfterContractsAreMinted"
* Wait for node "Node1" to catch up to "blockHeightAfterContractsAreMinted"
* Verify node "Node1" has the block height greater or equal to "blockHeightAfterContractsAreMinted"
* "Node1" is not able to seal new blocks
