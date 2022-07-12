# Multi-tenancy in a Quorum Network

 Tags: multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, networks/plugins::qbft-qlight-multitenancy, pre-condition/no-record-blocknumber


1. chainConfig.isMPS=true, New Tessera with no resident config
2. chainConfig.isMPS=true, New Tessera with resident config


3. chainConfig.isMPS=false, New Tessera with no resident config : existing tests
4. chainConfig.isMPS=false, New Tessera with resident config
6. chainConfig.isMPS=false, old Tessera



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

* Configure `"JPM Investment"` in authorization server to access `"Node1"` with:
    | scope                    |
    |--------------------------|
    | `rpc://eth_*`            |
    | `psi://JPM?self.eoa=0x0` |
* Configure `"JPM Settlement"` in authorization server to access `"Node1"` with:
    | scope                                          |
    |------------------------------------------------|
    | `rpc://eth_*`                                  |
    | `psi://JPM?node.eoa=JPM_ACC1&self.eoa=Wallet1` |
* Configure `"GS Investment"` in authorization server to access `"Node1"` with:
    | scope                   |
    |-------------------------|
    | `rpc://eth_*`           |
    | `psi://GS?self.eoa=0x0` |
* Configure `"GS Settlement"` in authorization server to access `"Node1"` with:
    | scope                                        |
    |----------------------------------------------|
    | `rpc://eth_*`                                |
    | `psi://GS?node.eoa=GS_ACC1&self.eoa=Wallet2` |

## JPM can only deploy private contracts from its owned privacy addresses and assigned Ethereum Accounts

tags: private, deploy, raw

 JPM who self-manages account keys use raw transaction flow to submit private transactions

* `"JPM Investment"` can deploy a "SimpleStorage" private contract to `"Node1"`, private from `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"` using `"JPM_ACC1"`, private from `"JPM_K1"` and private for `"JPM_K1"`
* `"JPM Investment"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"`, private from `"GS_K1"`, signed by `"Wallet1"` and private for `"GS_K2"`
* `"JPM Investment"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"` targeting `"GS"` tenancy

## GS can not access private contracts deployed by JPM and not for GS

tags: private, access, raw

 Noted that even GS can not access the contract state, GS can submit a transaction to change the contract state.
 In order to strengthen the control, privacy enhancement must be used.

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"GS Investment"` gets empty contract code for "contract1" on `"Node1"`
* `"GS Investment"` invokes get in "contract1" on `"Node1"` and gets value "0"
* `"DB Investment"` gets empty contract code for "contract1" on `"Node2"`
* `"DB Investment"` invokes get in "contract1" on `"Node2"` and gets value "0"

## JPM using self-managed account keys can only receive events from private contracts which are privy to it

tags: private, events, raw

 JPM who self-manages account keys use raw transaction flow to submit private transactions
 JPM and GS can access to events via the following methods:

 - Call `eth_getTransactionReceipt` API

 - Call `eth_getLogs` API

 - Create a filter via `eth_newFilter` and call `eth_getFilterLogs`

 - Use Pub/Sub API for `logs` subscription (TBD)

 In this scenario, `DB Investment` is not one of the participants hence it is not able to receive log events despite using any of the above methods

* `"JPM Investment"` deploys a "ClientReceipt" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K2, DB_K1"`

* `"JPM Settlement"`, initially allocated with key(s) `"JPM_K2"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionJPM"
* `"DB Investment"`, initially allocated with key(s) `"DB_K1"`, subscribes to log events from "contract1" in `"Node2"`, named this subscription as "subscriptionDB"
* `"GS Investment"`, initially allocated with key(s) `"GS_K1"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionGS"

* `"JPM Investment"` executes "contract1"'s `deposit()` function "5" times with arbitrary id and value between original parties

* "subscriptionJPM" receives "5" events
* `"JPM Settlement"` sees "5" events in total from transaction receipts in `"Node1"`
* `"JPM Settlement"` sees "5" events when filtering by "contract1" address in `"Node2"`

* "subscriptionDB" receives "5" events
* `"DB Investment"` sees "5" events in total from transaction receipts in `"Node2"`
* `"DB Investment"` sees "5" events when filtering by "contract1" address in `"Node2"`

* "subscriptionGS" receives "0" events
* `"GS Investment"` sees "0" events in total from transaction receipts in `"Node1"`
* `"GS Investment"` sees "0" events when filtering by "contract1" address in `"Node1"`

## JPM, GS and DB using self-managed account keys can access public states

tags: public, raw

* `"JPM Investment"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"`
* `"GS Investment"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node1"`
* `"DB Investment"` reads the value from "contract1" successfully by sending a request to `"Node2"` and the value returns "123"

## JPM, GS and DB using node-managed account keys can access public states

tags: public, node-managed-account

* `"JPM Investment"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"` using `"JPM_ACC1"`
* `"GS Settlement"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node1"` using `"GS_ACC1"`
* `"DB Investment"` reads the value from "contract1" successfully by sending a request to `"Node2"` and the value returns "123"
