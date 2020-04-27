# Block synchronization when using Raft consensus. Add a learner node and promote it. And test if raft's quorum is affected when nodes are stopped

  Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, gcmode, block-sync

  Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
  Setting `--gcmode=archive` would retain all historical data.

  This specification is to describe the expection w.r.t block synchronization for Quorum Network to function based on the following permutations

      |id    |networkType      |consensus|gcmode | nodeType |
      |raft9 |permissioned     |raft     |archive| learner  |

## Verify block synchronization

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

  This scenario is run against each row of data in the table above

* Start a <networkType> Quorum Network, named it <id>, consisting of "Node1,Node2,Node3" with <gcmode> `gcmode` using <consensus> consensus
* Blocks are synced when adding new node "Node4" with <gcmode> `gcmode` to network <id> as <nodeType>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract
* Stop node "Node4" in the network <id>
* Send some transactions to create blocks in network <id> from "Node1,Node2" and capture the latest block height as "latestBlockHeight"
* Verify node "Node3" has the block height greater or equal to "latestBlockHeight"
* Verify node "Node2" has the block height greater or equal to "latestBlockHeight"
* Save block height "latestBlockHeight" as "BlockHeight1"
* Stop node "Node3" in the network <id>
* Send some transactions to create blocks in network <id> from "Node1,Node2" and capture the latest block height as "latestBlockHeight"
* Verify block height "latestBlockHeight" is greater than "BlockHeight1"
* Verify node "Node2" has the block height greater or equal to "latestBlockHeight"
* Start node "Node4" in the network <id>
* Start node "Node3" in the network <id>
* Blocks are synced when promoting learner node "Node4" in network <id>
* Record the current block number, named it as "blockHeightBeforeStart"
* Stop all nodes in the network <id>
* Start all nodes in the network <id>
* Verify block heights in all nodes are greater or equals to "blockHeightBeforeStart" in the network <id>
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract

* Stop node "Node4" in the network <id>
* Send some transactions to create blocks in network <id> from "Node1,Node2" and capture the latest block height as "latestBlockHeight"
* Verify node "Node3" has the block height greater or equal to "latestBlockHeight"
* Verify node "Node2" has the block height greater or equal to "latestBlockHeight"
* Save block height "latestBlockHeight" as "BlockHeight2"
* Stop node "Node3" in the network <id>
* Deploy a simple smart contract from "Node1", verify it does not get mined
* Record the current block number, named it as "blockHeightAfterTxn"
* Verify block height "blockHeightAfterTxn" is equal to "BlockHeight2"
* Verify node "Node2" has the block height less than or equal to "blockHeightAfterTxn"


