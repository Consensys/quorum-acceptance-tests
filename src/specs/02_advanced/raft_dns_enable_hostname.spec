# Raft enable DNS in mixed network

  Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, raftdnsenable

  Quorum introduces `--raftdnsenable` since 2.4.0. This allows nodes to use hostname instead of ip to identify raft peers.
  However, there is a Raft incompatibility issue found in 2.4.0 when proposing a node with hostname in a mixed network of
  raftdnsenable and raftdnsdisable node.

## Check DNS Compatibility

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

  This scenario is to test:
  1) adding node 4 with its hostname
  2) test compatibility between dns enabled node 1,2,3 and dns disabled node 4
  3) raft.addPeer should work as expected and hostname should be displayed in raft.cluster
  (https://github.com/ConsenSys/quorum/pull/937)

* Start a Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "raftdnsenable" using raft consensus
* Add new raft peer "Node4" with "raftdnsdisable" to network "default"
* Propose new node from "Node4" with hostname expect error be "true"
* Propose new node from "Node1" with hostname expect error be "false"
* Restart all nodes in the network "default"
* Verify "Node1" has raft peer "5" with valid hostname in raft cluster
* Verify "Node4" has raft peer "5" with valid hostname in raft cluster
