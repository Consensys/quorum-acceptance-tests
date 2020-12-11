# Extend private contract to new party in a multi-tenant Quorum Network

 Tags: contract-extension, multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

In this spec, 4-node network allocates TM keys as below:
 - `Node1` manages
    - TM keys: `JPM_K1`, `JPM_K2`, `GS_K1`, `GS_K3`
    - Ethereum Accounts: `JPM_ACC1`, `JPM_ACC2`, `GS_ACC1`, `GS_ACC3`
 - `Node2` manages:
    - TM keys: `GS_K2`, `DB_K1`
    - Ethereum Accounts: `GS_ACC2`, `DB_ACC1`

JPM, GS and DB are able to transact with each other and can not contracts which are privy to other's TM keys.

[Limitation] JPM deploys contract C1 (JPM_K1, GS_K1 and DB_K1) between JPM, GS and DB. Then DB sends a private tx from DB_K1 to GS_K1: JPM sees the update C1

We run the scenarios with each of the following privacy flag
    | privacyFlag |
    | StandardPrivate |
    | PartyProtection |
    | StateValidation |

Tenants:
 - JPM with TM keys: `JPM_K1`, `JPM_K2` on `Node1`; with Ethereum Accounts `JPM_ACC1`, `JPM_ACC2`; with 3 clients
    - `JPM Investment` has `JPM_K1`, `JPM_ACC1`; Node1
    - `JPM Settlement` has `JPM_K2` and `JPM_ACC2`; Node1
    - `JPM Audit` has `READ` access to `JPM_K1`, `JPM_K2`, `JPM_ACC1`, `JPM_ACC2`; Node1
 - GS with TM keys: `GS_K1`, `GS_K2`, `GS_K3` on `Node1` and `Node2`; with Ethereum Accounts `GS_ACC1`, `GS_ACC2`, `GS_ACC3`; with 4 clients
    - GS Investment has `GS_K1` and `GS_ACC1`; Node1
    - GS Settlement has `GS_K2`, `GS_ACC2`; Node2
    - GS Research has `GS_K3` and `GS_ACC3`; Node1
    - GS Audit has READ access to `GS_K1`, `GS_K2`, `GS_K3`, `GS_ACC1`, `GS_ACC2`, `GS_ACC3`; Node1 and Node2
 - `DB` with TM key: `DB_K1` on `Node2`; with Ethereum Accounts `DB_ACC1`; with 1 client
    - `DB Investment` has `DB_K1`, `DB_ACC1`: Node2

* Configure `"JPM Investment"` in authorization server with:
    | scope                                                         |
    |---------------------------------------------------------------|
    | `rpc://eth_*`                                                 |
    | `rpc://quorumExtension_*`                                     |
    | `private://JPM_ACC1/_/contracts?owned.eoa=0x0&from.tm=JPM_K1` |
* Configure `"JPM Settlement"` in authorization server with:
    | scope                                                         |
    |---------------------------------------------------------------|
    | `rpc://eth_*` |
    | `private://JPM_ACC2/_/contracts?owned.eoa=0x0&from.tm=JPM_K2` |
* Configure `"JPM Audit"` in authorization server with:
    | scope                                                            |
    |------------------------------------------------------------------|
    | `rpc://eth_*`                                                    |
    | `private://0x0/read/contracts?owned.eoa=JPM_ACC1&from.tm=JPM_K1` |
    | `private://0x0/read/contracts?owned.eoa=JPM_ACC2&from.tm=JPM_K2` |
* Configure `"GS Investment"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
    | `rpc://quorumExtension_*`                                   |
    | `private://GS_ACC1/_/contracts?owned.eoa=0x0&from.tm=GS_K1` |
* Configure `"GS Settlement"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
    | `rpc://quorumExtension_*`                                   |
    | `private://GS_ACC2/_/contracts?owned.eoa=0x0&from.tm=GS_K2` |
* Configure `"GS Research"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
    | `rpc://quorumExtension_*`                                   |
    | `private://GS_ACC3/_/contracts?owned.eoa=0x0&from.tm=GS_K3` |
* Configure `"GS Audit"` in authorization server with:
    | scope                                                          |
    |----------------------------------------------------------------|
    | `rpc://eth_*`                                                  |
    | `private://0x0/read/contracts?owned.eoa=GS_ACC1&from.tm=GS_K1` |
    | `private://0x0/read/contracts?owned.eoa=GS_ACC2&from.tm=GS_K2` |
    | `private://0x0/read/contracts?owned.eoa=GS_ACC3&from.tm=GS_K3` |
* Configure `"DB Investment"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
    | `rpc://quorumExtension_*`                                   |
    | `private://DB_ACC1/_/contracts?owned.eoa=0x0&from.tm=DB_K1` |

## GS extends a contract to a new party in GS on different node, JPM and DB can not access the contract

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Research"'s `"GS_K3"`
  Verifying access based on access token
* `"GS Investment,GS Research"` can read "investmentContract" on `"Node1"`
* `"GS Settlement,DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`
* `"JPM Audit"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"` to `"GS Settlement"`'s `"GS_K2"` on `"Node2"` with acceptance by `"GS_ACC2"`
* `"GS Investment,GS Research"` can read "investmentContract" on `"Node1"`
* `"GS Settlement"` can read "investmentContract" on `"Node2"`
* `"JPM Audit"` can __NOT__ read "investmentContract" on `"Node1"`
* `"DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`

## GS extends a contract to a new party in GS on same node, JPM and DB can not access the contract

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Investment"'s `"GS_K1"`
  Verifying access based on access token
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"GS Settlement,DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`
* `"GS Research,JPM Audit"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"` to `"GS Research"`'s `"GS_K3"` on `"Node1"` with acceptance by `"GS_ACC3"`
* `"GS Investment,GS Research,"` can read "investmentContract" on `"Node1"`
* `"GS Settlement,DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`
* `"JPM Audit"` can __NOT__ read "investmentContract" on `"Node1"`

## GS extends a contract to a new party in JPM on the different node, DB can not access the contract

* `"GS Settlement"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node2"` private from `"GS_K2"` using `"GS_ACC2"`, named "researchContract", private for "GS Research"'s `"GS_K2"`
  Verifying access based on access token
* `"GS Settlement"` can read "researchContract" on `"Node2"`
* `"JPM Investment"` can __NOT__ read "researchContract" on `"Node1"`
  Extending contract and verifying
* `"GS Settlement"` extends "researchContract" on `"Node2"` private from `"GS_K2"` using `"GS_ACC2"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
* `"GS Settlement"` can read "researchContract" on `"Node2"`
* `"JPM Investment"` can read "researchContract" on `"Node1"`

## GS extends a contract to a new party in JPM on the same node, DB can not access the contract

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Research"'s `"GS_K3"`
  Verifying access based on access token
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"JPM Investment"` can __NOT__ read "investmentContract" on `"Node1"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
* `"GS Investment,JPM Investment"` can read "investmentContract" on `"Node1"`

## GS can not extend a contract using an unauthorized key and account

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Research"'s `"GS_K3"`
  Unauthorized party
* `"GS Investment"` "not authorized" to extend "investmentContract" on `"Node1"` private from `"GS_K2"` using `"GS_ACC1"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
  Unauthorized eth account
* `"GS Investment"` "not authorized" to extend "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC2"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`

## JPM can not accept the contract extension using unauthorized key and account

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Research"'s `"GS_K3"`
* Initiate `"investmentContract"` extension to `"JPM_K1"` received by `"JPM_ACC1"` in `"Node1"` from `"Node1"`, private from `"GS_K1"` using `"GS_ACC1"`
  Unauthorized party
* From `"Node1"` `"JPM Investment"` "not authorized" to accept `"investmentContract"` extension, private from `"JPM_K2"` using `"JPM_ACC1"` and private for `"GS_K1"`
  Unauthorized eth account
* From `"Node1"` `"JPM Investment"` "not authorized" to accept `"investmentContract"` extension, private from `"JPM_K1"` using `"JPM_ACC2"` and private for `"GS_K1"`