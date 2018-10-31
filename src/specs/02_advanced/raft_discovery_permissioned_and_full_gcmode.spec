# Block synchronization in permissioned network with full gcmode using Raft consensus

 Tags: advanced-1.18.12, permissioned, gcmode, full, sync, raft

Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
Setting `--gcmode=archive` would retain all historical data.

* Start a "permissioned" Quorum Network, named it "alpha", with "3" nodes with "full" `gcmode`s using "raft" consensus

## Block synchronization when adding new nodes

  Tags: add-nodes

* Blocks are synced when adding new node "Node4" with "full" `gcmode` to network "alpha"
* Blocks are synced when adding new node "Node5" with "full" `gcmode` to network "alpha"
* Blocks are synced when adding new node "Node6" with "full" `gcmode` to network "alpha"
* Verify privacy between "Node1" and "Node6" using a simple smart contract excluding "Node4"

## Block synchronization after retarting the network

  Tags: start, stop

* Stop all nodes in the network "alpha"
* Start all nodes in the network "alpha"
* Verify block heights in all nodes are the same in the network "alpha"
* Verify privacy between "Node1" and "Node6" using a simple smart contract excluding "Node4"

