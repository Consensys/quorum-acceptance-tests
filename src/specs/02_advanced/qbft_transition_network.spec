# QBFT consensus - Legacy Istanbul to QBFT Network

  Tags: networks/template::qbft-4nodes-transition, pre-condition/no-record-blocknumber, qbft-transition-network

This specification describes backwards compatiblity with legacy istanbul network
 - Starts with a 4 node Legacy Istanbul network
 - Transition to QBFT Consensus at a predifined block

## Verify transition network

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Start a Quorum Network, named it "mynetwork", consisting of "Node1,Node2,Node3,Node4"
* Send some transactions to create blocks in network "mynetwork" and capture the latest block height as "latestBlockHeight"
* Wait to catch up block "51"
