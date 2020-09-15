# Multi-tenancy in a Quorum Network

 Tags: multitenancy, networks/plugins::istanbul-rpc-security, networks/plugins::raft-rpc-security, pre-condition/no-record-blocknumber

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

   The setup can be depicted as below:

    ![image](http://www.plantuml.com/plantuml/png/ZP5FSu8m4CNl-HGxz8mEoSrXClBdsfuyUecmGcf8EY7KyTiNGqLWQlMsk-plxIDlspfXNTSeqfwa_X9MDD3MeZHdgIiu2jKY8GgF3fm1AwlQGIaj5auQJOYmHIqRuH3UBgYKEKEmGosqWoG0qK82SUomgBLHfhg49NxB0ZcVSfLHvKYbdagVKwrGZT4ZbtakUyvPSn7ge3g482xwDRivrz8X_zN9uxMVSg-NFz2R2kx6xn1yc18ZJ_ggfpyxxL-Ay_yOEZBqPOQD39ipZw3TUxJNOHZkTNmGnMapvCMJA5vVUxwSqyNycXDFbzj33ZoxpRsh_bJlgJ92SVxO3lLsvlBBesDSV9PM7jrocf-bBvnb_kpIFvqBiRiG6R9ShQw_vHiwDDGtR6WO8GdqkaFo9FspfYIRpEyr8_baPukwx1hr0m00)

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
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=A1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=A2` |

* Setup `"Tenant B"` access controls restricted to `"Node1, Node2"`, assigned TM keys `"B1,B2"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=B1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=B2` |

* Setup `"Tenant C"` access controls restricted to `"Node1, Node3"`, assigned TM keys `"C1,C2"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=C1` |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=C2` |

* Setup `"Tenant D"` access controls restricted to `"Node2"`, assigned TM keys `"D1"` and with the following scopes

  | scope                                                 |
  |-------------------------------------------------------|
  | `rpc://eth_*`                                         |
  | `rpc://rpc_modules`                                   |
  | `private://0x0/_/contracts?owned.eoa=0x0&party.tm=D1` |


## Tenants can only interact with the Quorum Nodes which are assigned to them

tags: rpc

* `"Tenant A"` can request for RPC modules available in `"Node1"`
* `"Tenant A"` can __NOT__ request for RPC modules available in `"Node2"`
* `"Tenant B"` can request for RPC modules available in `"Node1"`
* `"Tenant B"` can request for RPC modules available in `"Node2"`

## Tenants can only deploy private contracts from the allocated TM keys

tags: private, deploy

* `"Tenant A"` can deploy a "SimpleStorage" private contract to `"Node1"`, private from `"A1"` and private for `"A2"`
* `"Tenant A"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"`, private from `"B1"` and private for `"B2"`

## Tenants can only access to private contracts which are privy to them

tags: private, access

 `Tenant C` can not write to private contracts on which `C1` or `C2` are not participants

 `Tenant D` can not write to its private contract that manipulates other private contracts on which `D1` is not a participant

* `"Tenant A"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"A1"` and private for `"B2"`
* `"Tenant B"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node2"` with its TM key `"B2"` private for `"A1"`
* `"Tenant C"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"C1"` and private for `"B2"`
* `"Tenant D"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node2"` with its TM key `"D1"` and private for `"D1"`
* `"Tenant D"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node2"` with its TM key `"D1"` and private for `"D1"`

## Tenants can only receive events from private contracts which are privy to them

tags: private, events

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

## Tenants can access public states

tags: public

* `"Tenant A"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"`
* `"Tenant B"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node2"`
* `"Tenant C"` reads the value from "contract1" successfully by sending a request to `"Node3"` and the value returns "123"
