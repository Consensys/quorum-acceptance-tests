# Block synchronization when using Raft consensus - add/promote learner node

  Tags: networks/template::raft-3plus1, learner-add-promote, pre-condition/no-record-blocknumber, learner-peer-management

  Raft consensus supports learner node from Quorum 2.5.0 onwards.
  Learner node does not take part in Raft's consensus until it is promoted to a peer node.
  This spec verifies block synching in learner node once it is added and after it is promoted to a peer node.

      |id    |networkType      |consensus|gcmode | nodeType |
      |raft6 |permissioned     |raft     |archive| learner  |

## Verify block synchronization - add/promote learner node

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

  This scenario is run against each row of data in the table above

* Start a <networkType> Quorum Network, named it <id>, consisting of "Node1,Node2,Node3" with <gcmode> `gcmode` using <consensus> consensus
* Blocks are synced when adding new node "Node4" with <gcmode> `gcmode` to network <id> as <nodeType>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract
* Blocks are synced when promoting learner node "Node4" in network <id>
* Record the current block number, named it as "blockHeightBeforeStart"
* Stop all nodes in the network <id>
* Start all nodes in the network <id>
* Verify block heights in all nodes are greater or equals to "blockHeightBeforeStart" in the network <id>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract
