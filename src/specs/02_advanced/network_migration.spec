# Network migration from one version to the later version of `geth`

 Tags: networks/template::raft-4nodes, networks/template::istanbul-4nodes, migration, pre-condition/no-record-blocknumber

In this spec, we assume that all nodes in the network are initially in the same version and together
upgradable to the new version

        | from_version | to_version |
        | v2.5.0       | develop     |

* Start the network with:
    | node  | quorum         | tessera |
    |-------|----------------|---------|
    | Node1 | <from_version> | develop  |
    | Node2 | <from_version> | develop  |
    | Node3 | <from_version> | develop  |
    | Node4 | <from_version> | develop  |
* Use SimpleStorage smart contract, populate network with "500" public transactions randomly between "Node1,Node2,Node3,Node4"
* Record the current block number, named it as "recordedBlockNumber"

## Migrate all nodes in the network at the same time

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Restart the network with:
    | node  | quorum       | tessera |
    |-------|--------------|---------|
    | Node1 | <to_version> | develop  |
    | Node2 | <to_version> | develop  |
    | Node3 | <to_version> | develop  |
    | Node4 | <to_version> | develop  |
* Verify block number in "Node1,Node2,Node3,Node4" in sync with "recordedBlockNumber"
* Use SimpleStorage smart contract, populate network with "10" public transactions randomly between "Node1,Node2,Node3,Node4"
* Network is running

## Migrate node by node in the network

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Stop and start "quorum" in "Node1" using <to_version>
* Verify block number in "Node1" in sync with "recordedBlockNumber"
* Stop and start "quorum" in "Node2" using <to_version>
* Verify block number in "Node2" in sync with "recordedBlockNumber"
* Stop and start "quorum" in "Node3" using <to_version>
* Verify block number in "Node3" in sync with "recordedBlockNumber"
* Stop and start "quorum" in "Node4" using <to_version>
* Verify block number in "Node4" in sync with "recordedBlockNumber"
* Use SimpleStorage smart contract, populate network with "10" public transactions randomly between "Node1,Node2,Node3,Node4"
* Network is running
