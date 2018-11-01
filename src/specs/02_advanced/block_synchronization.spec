# Block synchronization

  Tags: advanced-1.8.12, sync, isolate, network-cleanup-required

  Geth 1.8.12 introduces `--gcmode=full/archive`. This controls trie pruning which is enabled by default on all `--syncmode`.
  Setting `--gcmode=archive` would retain all historical data.

  This specification is to describe the expection w.r.t block synchronization for Quorum Network to function based on the following permutations

      |id     |networkType      |consensus|gcmode |
      |alpha1 |permissioned     |raft     |full   |
      |alpha2 |permissioned     |raft     |archive|
      |alpha3 |permissioned     |istanbul |full   |
      |alpha4 |permissioned     |istanbul |archive|
      |alpha5 |non-permissioned |raft     |full   |
      |alpha6 |non-permissioned |raft     |archive|
      |alpha7 |non-permissioned |istanbul |full   |
      |alpha8 |non-permissioned |istanbul |archive|

* Note: this is not yet implemented and therefore skipped

## Verify block synchronization

* Block synchronization in <networkType> network, named it <id>, with <gcmode> `gcmode` using <consensus> consensus

---
Clean up the network is done via execution hook which is setup for `network-cleanup-required` tag