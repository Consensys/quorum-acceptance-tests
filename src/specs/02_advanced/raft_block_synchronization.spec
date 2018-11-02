# Block synchronization in Raft

  Tags: advanced-1.8.12, sync, isolate, network-cleanup-required, raft

  Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
  Setting `--gcmode=archive` would retain all historical data.

  This specification is to describe the expection w.r.t block synchronization for Quorum Network to function based on the following permutations

      |id    |networkType      |consensus|gcmode |
      |raft1 |permissioned     |raft     |full   |
      |raft2 |permissioned     |raft     |archive|
      |raft3 |non-permissioned |raft     |full   |
      |raft4 |non-permissioned |raft     |archive|

* Note: this is not yet implemented and therefore skipped

## Verify block synchronization

  Tags: add, start, stop

* Start a <networkType> Quorum Network, named it <id>, with "3" nodes with <gcmode> `gcmode`s using <consensus> consensus
* Blocks are synced when adding new node "Node4" with <gcmode> `gcmode` to network <id>
* Blocks are synced when adding new node "Node5" with <gcmode> `gcmode` to network <id>
* Blocks are synced when adding new node "Node6" with <gcmode> `gcmode` to network <id>
* Verify privacy between "Node1" and "Node6" using a simple smart contract excluding "Node4"
* Stop all nodes in the network <id>
* Start all nodes in the network <id>
* Verify block heights in all nodes are the same in the network <id>
* Verify privacy between "Node1" and "Node6" using a simple smart contract excluding "Node4"

---
Clean up the network is done via execution hook which is setup for `network-cleanup-required` tag