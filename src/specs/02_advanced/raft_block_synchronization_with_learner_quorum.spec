# Block synchronization when using Raft consensus. Verify raft's quorum with learner node.

  Tags: networks/template::raft-3plus1, learner-quorum, pre-condition/no-record-blocknumber, learner-peer-management

  Raft consensus supports learner node from Quorum 2.5.0 onwards.
  Learner node does not take part in Raft's consensus until it is promoted to a peer node.
  This spec verifies the following scenarios:
   Scenario 1:
   add learner node to a 3 node network. Stop 2 nodes and confirm the quorum of raft network is not affected as learner node is not counted.
   Scenario 2:
   following scenarion 2 - promote learner to peer node. Stop 2 nodes and confirm the quorum of raft network is affected as there are 4 peer nodes in the network now.

      |id    | nodeType |
      |raft9 | learner  |

## Verify block synchronization - raft's quorum with learner node

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

  This scenario is run against each row of data in the table above

* Start a Quorum Network, named it <id>, consisting of "Node1,Node2,Node3"
* Blocks are synced when adding new node "Node4" to network <id> as <nodeType>
* Blocks are synced when stopping node "Node4" and "Node3" in network <id> as learner node does not affect raft quorum
* Blocks are synced when promoting learner node "Node4" in network <id>
* Blocks are not synced when stopping node "Node4" and "Node3" in network <id> after learner node is promoted