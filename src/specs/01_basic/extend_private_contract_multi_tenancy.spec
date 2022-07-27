# Extend private contract to new party in a multi-tenant Quorum Network

 Tags: contract-extension, multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

In the 4-node network:
 - Multi-tenant `Node1` is shared between tenant `JPM` and `GS` (isMPS=true)
 - Standalone `Node2` is used by `DB` alone (isMPS=false)

Tenants:
 - `JPM` is allocated with TM keys `JPM_K1` and `JPM_K2`
 - `GS` is allocated with TM keys `GS_K1` and `GS_K2`
 - `DB` uses TM key `DB_K1`

Clients:
 - `JPM Investment` has access to `JPM` tenancy using any self-managed Ethereum Accounts
 - `JPM Settlement` has access to `JPM` tenancy using node-managed Ethereum Account `JPM_ACC1` and an self-managed one `Wallet1`
 - `GS Investment` has access to `GS` tenancy using any self-managed Ethereum Accounts
 - `GS Settlement` has access to `GS` tenancy using node-managed Ethereum Account `GS_ACC1` and an self-managed one `Wallet2`

We run the scenarios with each of the following privacy flag
    | privacyFlag |
    | STANDARD_PRIVATE |
    | PARTY_PROTECTION |
    | PRIVATE_STATE_VALIDATION |

* Configure `"JPM Investment"` in authorization server to access `"Node1"` with:
    | scope                    |
    |--------------------------|
    | `rpc://eth_*`            |
    | `rpc://quorumExtension_*`            |
    | `psi://JPM?self.eoa=0x0` |
* Configure `"JPM Settlement"` in authorization server to access `"Node1"` with:
    | scope                                          |
    |------------------------------------------------|
    | `rpc://eth_*`                                  |
    | `rpc://quorumExtension_*`            |
    | `psi://JPM?node.eoa=JPM_ACC1&self.eoa=Wallet1` |
* Configure `"GS Investment"` in authorization server to access `"Node1"` with:
    | scope                   |
    |-------------------------|
    | `rpc://eth_*`           |
    | `rpc://quorumExtension_*`            |
    | `psi://GS?self.eoa=0x0&node.eoa=GS_ACC1` |
* Configure `"GS Settlement"` in authorization server to access `"Node1"` with:
    | scope                                        |
    |----------------------------------------------|
    | `rpc://eth_*`                                |
    | `rpc://quorumExtension_*`            |
    | `psi://GS?node.eoa=GS_ACC1&self.eoa=Wallet2` |

## GS extends a contract to a new party in on the same node

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Settlement"'s `"GS_K2"`
  Verifying access based on access token
* `"GS Investment,GS Settlement"` can read "investmentContract" on `"Node1"`
* `"JPM Investment"` can __NOT__ read "investmentContract" on `"Node1"`
* `"DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"` to `"JPM Settlement"`'s `"JPM_K2"` on `"Node1"` with acceptance by `"JPM_ACC1"`
* `"GS Investment,GS Settlement,JPM Settlement,JPM Investment"` can read "investmentContract" on `"Node1"`
* `"DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`

## GS extends a contract to a new party in on the different node

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Investment"'s `"GS_K1"`
  Verifying access based on access token
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"JPM Investment"` can __NOT__ read "investmentContract" on `"Node1"`
* `"DB Investment"` can __NOT__ read "investmentContract" on `"Node2"`
  Extending contract and verifying
* `"GS Investment"` extends "investmentContract" on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"` to `"DB Investment"`'s `"DB_K1"` on `"Node2"` with acceptance by `"DB_ACC1"`
* `"GS Investment"` can read "investmentContract" on `"Node1"`
* `"DB Investment"` can read "investmentContract" on `"Node2"`
* `"JPM Investment"` can __NOT__ read "investmentContract" on `"Node1"`

## GS can not extend a contract using an unauthorized key and account

* `"GS Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node1"` private from `"GS_K1"` using `"GS_ACC1"`, named "investmentContract", private for "GS Investment"'s `"GS_K1"`
  Unauthorized party
* `"GS Investment"` "not authorized" to extend "investmentContract" on `"Node1"` private from `"JPM_K1"` using `"GS_ACC1"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`
  Unauthorized eth account
* `"GS Investment"` "not authorized" to extend "investmentContract" on `"Node1"` private from `"GS_K1"` using `"JPM_ACC1"` to `"JPM Investment"`'s `"JPM_K1"` on `"Node1"` with acceptance by `"JPM_ACC1"`

## JPM can not accept the contract extension using unauthorized key and account

* `"DB Investment"` deploys a <privacyFlag> `SimpleStorage` contract on `"Node2"` private from `"DB_K1"` using `"DB_ACC1"`, named "investmentContract", private for "DB Investment"'s `"DB_K1"`
* Initiate `"investmentContract"` extension to `"GS_K1"` received by `"GS_ACC1"` in `"Node1"` from `"Node2"`, private from `"DB_K1"` using `"DB_ACC1"`
  Unauthorized party
* From `"Node1"` `"GS Investment"` "not authorized" to accept `"investmentContract"` extension, private from `"JPM_K1"` using `"GS_ACC1"` and private for `"DB_K1"`
  Unauthorized eth account
* From `"Node1"` `"GS Investment"` "not authorized" to accept `"investmentContract"` extension, private from `"GS_K1"` using `"JPM_ACC1"` and private for `"DB_K1"`
  Non tenant
* From `"Node1"` `"JPM Investment"` "not authorized" to accept `"investmentContract"` extension, private from `"JPM_K1"` using `"JPM_ACC1"` and private for `"DB_K1"`
