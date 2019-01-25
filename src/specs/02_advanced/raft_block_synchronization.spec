# Block synchronization when using Raft consensus

  Tags: advanced-1.8.12, sync, isolate, network-cleanup-required, raft

  Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
  Setting `--gcmode=archive` would retain all historical data.

  This specification is to describe the expection w.r.t block synchronization for Quorum Network to function based on the following permutations

      |id    |networkType      |consensus|gcmode |
      |raft1 |permissioned     |raft     |full   |
      |raft2 |permissioned     |raft     |archive|
      |raft3 |non-permissioned |raft     |full   |
      |raft4 |non-permissioned |raft     |archive|

  `quorum-tools` is needed in order to run this specification with command `qctl quorum boot`. `boot-endpoint` in `application-local.yml` must be configured accordingly

## Verify block synchronization

  Tags: add, start, stop, network-setup

  This scenario is run against each row of data in the table above

* Start a <networkType> Quorum Network, named it <id>, with "3" nodes with <gcmode> `gcmode` using <consensus> consensus
* Blocks are synced when adding new node "Node4" with <gcmode> `gcmode` to network <id>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract
* Stop all nodes in the network <id>
* Start all nodes in the network <id>
* Verify block heights in all nodes are the same in the network <id>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract

---
Clean up the network is done via execution hook which is setup for `network-cleanup-required` tag