# Block synchronization in Istanbul

  Tags: advanced-1.8.12, sync, isolate, network-cleanup-required, istanbul

  Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
  Setting `--gcmode=archive` would retain all historical data.

  This specification is to describe the expection w.r.t block synchronization for Quorum Network to function based on the following permutations

      |id        |networkType      |consensus|gcmode |
      |istanbul1 |permissioned     |istanbul |full   |


## Verify block synchronization

  Tags: add, start, stop, network-setup

* Start a <networkType> Quorum Network, named it <id>, with "4" nodes with <gcmode> `gcmode` using <consensus> consensus
* Blocks are synced when adding new node "Node5" with <gcmode> `gcmode` to network <id>
* "Node5" is able to seal new blocks
* Blocks are synced when adding new node "Node6" with <gcmode> `gcmode` to network <id>
* "Node6" is able to seal new blocks
* Blocks are synced when adding new node "Node7" with <gcmode> `gcmode` to network <id>
* "Node7" is able to seal new blocks
* Verify privacy between "Node1" and "Node7" excluding "Node4" when using a simple smart contract
* Stop all nodes in the network <id>
* Start all nodes in the network <id>
* Verify block heights in all nodes are the same in the network <id>
* Verify privacy between "Node1" and "Node7" excluding "Node4" when using a simple smart contract

---
Clean up the network is done via execution hook which is setup for `network-cleanup-required` tag