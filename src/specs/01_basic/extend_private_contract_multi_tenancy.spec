# Extend private contract to new party in a multi-tenant Quorum Network

 Tags: contract-extension, multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

Please refer to multi_tenancy.spec for high-level description of multitenancy.

In this spec, 4-node network allocates TM keys as below:
 - Node1 manages JPM_K1, JPM_K2, GS_K1, GS_K3
 - Node2 manages GS_K2, DB_K1

In this spec, 4-node network allocates TM keys as below:
 - Node1 manages
    - TM keys: JPM_K1, JPM_K2, GS_K1, GS_K3
    - Ethereum Accounts: JPM_ACC1, JPM_ACC2, GS_ACC1, GS_ACC3
 - Node2 manages:
    - TM keys: GS_K2, DB_K1
    - Ethereum Accounts: GS_ACC2, DB_ACC1
 
Tenants:
 - JPM with TM keys: JPM_K1, JPM_K2 on Node1; with Ethereum Accounts JPM_ACC1, JPM_ACC2; with 3 clients
    - JPM Investment has JPM_K1 and JPM_ACC1
    - JPM Settlement has JPM_K2 and JPM_ACC2
    - JPM Audit has READ access to JPM_K1, JPM_K2, JPM_ACC1, JPM_ACC2
 - GS with TM keys: GS_K1, GS_K2, GS_K3 on Node1 and Node2; with Ethereum Accounts GS_ACC1, GS_ACC2, GS_ACC3; with 4 clients
    - GS Investment has GS_K1 and GS_ACC1
    - GS Settlement has GS_K2 and GS_ACC2
    - GS Research has GS_K3 and GS_ACC3
    - GS Audit has READ access to GS_K1, GS_K2, GS_K3, GS_ACC1, GS_ACC2, GS_ACC3
 - DB with TM key: DB_K1 on Node 2; with Ethereum Accounts DB_ACC1; with 1 client
    - DB Investment has DB_K1 and DB_ACC1
 
JPM, GS and DB are able to transact with each other and can not contracts which are privy to other's TM keys.

[Limitation] JPM deploys contract C1 (JPM_K1, GS_K1 and DB_K1) between JPM, GS and DB. Then DB sends a private tx from DB_K1 to GS_K1: JPM sees the update C1

We run the scenarios with each of the following privacy flag
    | privacyFlag |
    | StandardPrivate |
    | PartyProtection |
    | StateValidation |


* Setup `"JPM"` access controls restricted to `"Node1"`, assigned TM keys `"JPM_K1,JPM_K2"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=JPM_K1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=JPM_K2` |

* Setup `"GS"` access controls restricted to `"Node1, Node2"`, assigned TM keys `"GS_K1,GS_K2,GS_K3"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=GS_K1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=GS_K2` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=GS_K3` |

* Setup `"DB"` access controls restricted to `"Node2"`, assigned TM keys `"DB_K1"` and with the following scopes

  | scope                                                |
  |------------------------------------------------------|
  | `rpc://eth_*`                                        |
  | `rpc://quorumExtension_*`                            |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=DB_K1` |

## GS extends a contract to a new party in GS on different node, JPM and DB can not access the contract

* `"GS Investment"` deploys a <privacyTag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"`, named "investmentContract", private for "GS Research"'s `"GS_K3"`
  Verifying access based on access token
* `"GS Investment,GS Research"` can read "investmentContract" on `"Node1"`
* `"GS Settlement"` can __NOT__ read "investmentContract" on `"Node2"`
* `"JPM Audit,DB Investment"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"` to `"GS Research"`'s `"GS_K2"` on `"Node2"` with acceptance by `"GS_ACC2"`
* `"GS Investment,GS Research,GS Settlement"` can read "investmentContract" on `"Node1"`
* `"JPM Audit,DB Investment"` can __NOT__ read "investmentContract" on `"Node1"`

## GS extends a contract to a new party in GS on same node, JPM and DB can not access the contract

* `"GS Investment"` deploys a <privacyTag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"`, named "investmentContract", private for "GS Investment"'s `"GS_K1"`
  Verifying access based on access token
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"GS Settlement"` can __NOT__ read "investmentContract" on `"Node2"`
* `"GS Research,JPM Audit,DB Investment"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"` to `"GS Research"`'s `"GS_K3"` on `"Node1"` with acceptance by `"GS_ACC3"`
* `"GS Investment,GS Research"` can read "investmentContract" on `"Node1"`
* `"GS Settlement"` can __NOT__ read "investmentContract" on `"Node2"`
* `"JPM Audit,DB Investment"` can __NOT__ read "investmentContract" on `"Node1"`

## GS extends a contract to a new party in JPM on the different node, DB can not access the contract

* `"GS Research"` deploys a <privacyTag> `SimpleStorage` contract on `"Node2"` private from `"GS_K2"` signed by `"GS_ACC2"`, named "researchContract", private for "GS Research"'s `"GS_K2"`
  Verifying access based on access token
* `"GS Research"` can read "researchContract" on `"Node2"`
* `"JPM Investment"` can __NOT__ read "researchContract" on `"Node1"`
  Extending contract and verifying
* `"GS Research"` extends "researchContract" on `"Node2"` private from `"GS_K2"` signed by `"GS_ACC2"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
* `"GS Research"` can read "researchContract" on `"Node2"`
* `"JPM Investment"` can read "researchContract" on `"Node1"`

## GS extends a contract to a new party in JPM on the same node, DB can not access the contract

* `"GS Investment"` deploys a <privacyTag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"`, named "investmentContract", private for "GS Investment"'s `"GS_K3"`
  Verifying access based on access token
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"JPM Investment"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` signed by `"GS_ACC1"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
* `"GS Investment,JPM Investment"` can read "investmentContract" on `"Node1"`

## GS can not extend a contract using an unauthorized key and account
    fail in subitting contract extension request

    Target JPM Investment JPM_K1, JPM_ACC1

 | Client  | Private From | Sign Account | Desc |
 |----------|--------------|--------------|------|
 | "GS Investmmentt" | "GS_K2" | "GS_ACC1" | Unauthorized Private From |
 | "GS Investmmentt" | "GS_K1" | "GS_ACC2" | Unauthorized Sign Account |

## JPM can not accept the contract extension using unauthorized key and account
  successful submitting contract extension rerquest but fail in accepting the request
  "GS Investmmentt" | "GS_K1" | "GS_ACC1" -> JPM Investment, JPM_K1, JPM_ACC1

  | Client   | Private From | Sign Account | Desc |
  |----------|--------------|--------------|------|
  | "JPM Investmmentt" | "JPM_K2" | "JPM_ACC1" | Unauthorized Private From |
  | "JPM Investmmentt" | "JPM_K1" | "JPM_ACC2" | Unauthorized Sign Account |