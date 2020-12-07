# Multi-tenancy in a Quorum Network

 Tags: multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

 __OAuth2 Authorization__

 Authorization Server must be an [OAuth2](https://tools.ietf.org/html/rfc6749)-compliant server.
 There are many existing services both from commercial and open source.
 We use a reference implementation to demonstrate the basic OAuth2 client credentials grant flow through out this spec.

 Client application is expected to authenticate against an Authorization Server in order to obtain a bearer token
 which must be included in all requests sent to a Quorum node via standard `Authorization` HTTP header.

 __Use cases__

 For the coming test scenarios, we setup the following:

 - A Quorum Network consists of 3 security-enabled Quorum nodes (`Node1`, `Node2` and `Node3`) paired with 3 corresponding
 Private Transaction Managers (`TM1`, `TM2` and `TM3`) respectively.

 - There are 4 tenants: `Tenant A`, `Tenant B`, `Tenant C` and `Tenant D` using this network.

    - `Tenant A` is assigned to `Node1/TM1` with TM key `A1` and `A2`

    - `Tenant B` is assigned to `Node1/TM1` with TM key `B1` and to `Node2/TM2` with TM key `B2`

    - `Tenant C` is assigned to `Node1/TM1` with TM key `C1` and to `Node3/TM3` with TM key `C2`

    - `Tenant D` is assigned to `Node2/TM2` with TM key `D1`

   Which means:

    - `TM1` manages keys: `A1`, `A2`, `B1` and `C1`

    - `TM2` manages keys: `B2` and `D1`

    - `TM3` manages keys: `C2`

 __Onboarding tenants__

 In this context, onboarding activities are Authorization Server configuration activities so the access controls are setup
 for tenants, that includes: tenant identity, app-to-app credentials, etc ..., so their applications can authenticate
 and start interacting with the network.

 The configuration steps varies between Authorization Server implementations. We only use our reference implementation to
 show case the fundamental configuration.


* Setup `"Tenant A"` access controls restricted to `"Node1"`, assigned TM keys `"A1,A2"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=A1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=A2` |

* Setup `"Tenant B"` access controls restricted to `"Node1, Node2"`, assigned TM keys `"B1,B2"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=B1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=B2` |

* Setup `"Tenant C"` access controls restricted to `"Node1, Node3"`, assigned TM keys `"C1,C2"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=C1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=C2` |

* Setup `"Tenant D"` access controls restricted to `"Node2"`, assigned TM keys `"D1"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&from.tm=D1` |


## Tenants can only interact with the Quorum Nodes which are assigned to them

tags: rpc

* `"Tenant A"` can request for RPC modules available in `"Node1"`
* `"Tenant A"` can __NOT__ request for RPC modules available in `"Node2"`
* `"Tenant B"` can request for RPC modules available in `"Node1"`
* `"Tenant B"` can request for RPC modules available in `"Node2"`

## Tenants using self-managed account keys can only deploy private contracts from the allocated TM keys

tags: private, deploy, raw

 Tenants who self-manage account keys use raw transaction flow to submit private transactions

* `"Tenant A"` can deploy a "SimpleStorage" private contract to `"Node1"`, private from `"A1"` and private for `"A2"`
* `"Tenant A"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"`, private from `"B1"` and private for `"B2"`

## Tenants using self-managed account keys can only access to private contracts which are privy to them

tags: private, access, raw

 Tenants who self-manage account keys use raw transaction flow to submit private transactions
 `Tenant B` can not write to private contracts on which `B1` or `B2` are not participants
 `Tenant B` fails when attempting to use its private contract that manipulates other private contracts on which `B1` or `B2` are not participants

* `"Tenant A"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"A1"` and private for `"A2"`
* `"Tenant A"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"A1"` private for `"A2"`
* `"Tenant A"` can read "contract1" from "Node1"
* `"Tenant B"` fails to read "contract1" from "Node1"
* `"Tenant B"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"A1"`
* `"Tenant B"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`
* `"Tenant B"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`

## Tenants using self-managed account keys can only receive events from private contracts which are privy to them

tags: private, events, raw

 Tenants who self-manage account keys use raw transaction flow to submit private transactions
 Tenants can access to events via the following methods:

 - Call `eth_getTransactionReceipt` API

 - Call `eth_getLogs` API

 - Create a filter via `eth_newFilter` and call `eth_getFilterLogs`

 - Use Pub/Sub API for `logs` subscription (TBD)

 In this scenario, `Tenant D` is not one of the participants hence it is not able to receive log events despite using any of the above methods

* `"Tenant A"` deploys a "ClientReceipt" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"A2"` and private for `"B2, C2"`
* `"Tenant D"`, initially allocated with key(s) `"D1"`, subscribes to log events from "contract1" in `"Node2"`, named this subscription as "subscriptionD"
* `"Tenant A"`, initially allocated with key(s) `"A1,A2"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionA"
* `"Tenant A"` executes "contract1"'s `deposit()` function "5" times with arbitrary id and value between original parties
* "subscriptionA" receives "5" events
* `"Tenant B"`, initially allocated with key(s) `"B2"`, sees "5" events in total from transaction receipts in `"Node2"`
* `"Tenant C"`, initially allocated with key(s) `"C2"`, sees "5" events when filtering by "contract1" address in `"Node3"`
* "subscriptionD" receives "not authorized" error
* `"Tenant D"`, initially allocated with key(s) `"D1"`, sees "not authorized" when retrieving logs from transaction receipts in `"Node2"`
* `"Tenant D"`, initially allocated with key(s) `"D1"`, sees "not authorized" when filtering logs by "contract1" address in `"Node2"`

## Tenants using node-managed account keys can only deploy private contracts from the allocated TM keys

tags: private, deploy, node-managed-account

* `"Tenant A"` can deploy a "SimpleStorage" private contract to `"Node1"` using node's default account, private from `"A1"` and private for `"A2"`
* `"Tenant A"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"` using node's default account, private from `"B1"` and private for `"B2"`

## Tenants using node-managed account keys can only access to private contracts which are privy to them

tags: private, access, node-managed-account

 `Tenant B` can not write to private contracts on which `B1` or `B2` are not participants
 `Tenant B` fails when attempting to use its private contract that manipulates other private contracts on which `B1` or `B2` are not participants

* `"Tenant A"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"A1"` using node's default account and private for `"A2"`
* `"Tenant A"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"A1"` private for `"A2"`
* `"Tenant A"` can read "contract1" from "Node1"
* `"Tenant B"` fails to read "contract1" from "Node1"
* `"Tenant B"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"B1"` using node's default account and private for `"B2"`
* `"Tenant B"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"B1"` using node's default account and private for `"B2"`
* `"Tenant B"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`

## Tenants using self-managed account keys can access public states

tags: public, raw

* `"Tenant A"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"`
* `"Tenant B"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node2"`
* `"Tenant C"` reads the value from "contract1" successfully by sending a request to `"Node3"` and the value returns "123"

## Tenants using node-managed account keys can access public states

tags: public, node-managed-account

* `"Tenant A"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"` using node's default account
* `"Tenant B"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node2"` using node's default account
* `"Tenant C"` reads the value from "contract1" successfully by sending a request to `"Node3"` and the value returns "123"

## Tenants using self-managed account keys can NOT bypass access controls to private contracts which are not privy to by using nonce shifts

tags: private, access, raw

 Tenants who self-manage account keys use raw transaction flow to submit private transactions
 `Tenant B` can not write to private contracts on which `B1` or `B2` are not participants
 `Tenant B` fails when attempting to use its private contract that manipulates other private contracts on which `B1` or `B2` are not participants

* `"Tenant A"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"A1"` and private for `"A2"`
* `"Tenant A"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"A1"` private for `"A2"`
* `"Tenant A"` can read "contract1" from "Node1"
* `"Tenant B"` fails to read "contract1" from "Node1"
* `"Tenant B"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"A1"`
* `"Tenant B"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`
* `"Tenant B"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`
 Tenant B sends two transactions (the first one has a nonce higer than the second):
    1. TX1 - getFromDelegate with nonce=accountTxCount+1
    2. TX2 - setDelegate with noce=accountTxCount
 TX1 cannot be minted as there is a nonce gap so it goes to the TX pool in pending status. During simulation the "delegate" property is false so there is
 no attempt from sneakyDelegateContract to interact with contract1 (so everything passes validation).
 When TX2 is submitted - they both become "available" in the tx pool and are minted in the next block. The transaction order in the block is TX2, TX1.
 This means that when the block is executed the "delegate" property in the SneakyWrapper is true and sneakyDelegateContract interacts (and reads) the
 value from contract1 (and sets the value in it's own local variable).
 This highlights that simulation checks ARE NOT ENOUGH to ensure the behavior of multiple transactions (when mined together).
 By comparisson privacy enhancements performs checks in both phases (tx sumbmittion and new block appended) and does not apply transactions if the checks
 are not satisfied.
* `"Tenant B"` deploys a "SneakyWrapper(contract1)" private contract, named "sneakyDelegateContract", by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"`
* `"Tenant B"` invokes getFromDelgate with nonce shift "1" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"B1"` and private for `"B2"` name this transaction "sneakyDelegateContract_TX1"
* `"Tenant B"` invokes setDelegate to "true" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"B1"` private for `"B2"`
* `"Tenant B"` checks the transaction "sneakyDelegateContract_TX1" on `"Node1"` has failed
 After adding post execution checks the SneakyWrapper is unable to copy the data from contract1
* `"Tenant B"` invokes get in "sneakyDelegateContract" on `"Node1"` and gets value "0"
