# Extend private contract to new party in a multi-tenant Quorum Network

 Tags: contract-extension, multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

Please refer to multi_tenancy.spec for high-level description of multitenancy.

In this spec, 4-node network allocates TM keys as below:
 - Node1 manages A1, A2, B1, B3
 - Node2 manages B2, D1

* Setup `"Tenant A"` access controls restricted to `"Node1"`, assigned TM keys `"A1,A2"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=A1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=A2` |

* Setup `"Tenant B"` access controls restricted to `"Node1, Node2"`, assigned TM keys `"B1,B2,B3"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=B1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=B2` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=B3` |

* Setup `"Tenant D"` access controls restricted to `"Node2"`, assigned TM keys `"D1"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=D1` |

## A tenant extends a contract to a new party, other tenants must not be able to access

This is to verify that tentants' state are isolated in the context of contract extension.
This test case is important as the contract extension's implementation indeed
copies states rather than replaying the transactions.

* `"Tenant B"` deploys a "SimpleStorage" private contract, named "contractB", by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B3"`
* `"Tenant B"` extends contract "contractB" from `"Node1"` to `"B2"` in `"Node2"` with acceptance
* `"Tenant B"` can read contract "contractB" from `"Node2"`
* `"Tenant A"` can __NOT__ read contract "contractB" from `"Node1"`
* `"Tenant D"` can __NOT__ read contract "contractB" from `"Node2"`

