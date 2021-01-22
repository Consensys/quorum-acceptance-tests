# Multi-tenancy in a Quorum Network

 Tags: multitenancy, networks/plugins::raft-multitenancy, networks/plugins::istanbul-multitenancy, pre-condition/no-record-blocknumber

In this spec, 4-node network allocates TM keys as below:
 - `Node1` manages
    - TM keys: `JPM_K1`, `JPM_K2`, `GS_K1`, `GS_K3`
    - Ethereum Accounts: `JPM_ACC1`, `JPM_ACC2`, `GS_ACC1`, `GS_ACC3`
 - `Node2` manages:
    - TM keys: `GS_K2`, `DB_K1`
    - Ethereum Accounts: `GS_ACC2`, `DB_ACC1`

Tenants:
 - JPM with TM keys: `JPM_K1`, `JPM_K2` on `Node1`; with Ethereum Accounts `JPM_ACC1`, `JPM_ACC2`; with 3 clients
    - `JPM Investment` has `JPM_K1`, `JPM_ACC1` and an external Etherum Account in `Wallet1`
    - `JPM Settlement` has `JPM_K2` and `JPM_ACC2`
    - `JPM Audit` has `READ` access to `JPM_K1`, `JPM_K2`, `JPM_ACC1`, `JPM_ACC2`
 - GS with TM keys: `GS_K1`, `GS_K2`, `GS_K3` on `Node1` and `Node2`; with Ethereum Accounts `GS_ACC1`, `GS_ACC2`, `GS_ACC3`; with 4 clients
    - GS Investment has `GS_K1` and `GS_ACC1` and an external Etherum Account in `Wallet2`
    - GS Settlement has `GS_K2`, `GS_ACC2` and an external Etherum Account in `Wallet2`
    - GS Research has `GS_K3` and `GS_ACC3`
    - GS Audit has READ access to `GS_K1`, `GS_K2`, `GS_K3`, `GS_ACC1`, `GS_ACC2`, `GS_ACC3`
 - `DB` with TM key: `DB_K1` on `Node2`; with Ethereum Accounts `DB_ACC1`; with 1 client
    - `DB Investment` has `DB_K1`, `DB_ACC1` and an external Etherum Account in `Wallet8`

* Configure `"JPM Investment"` in authorization server with:
    | scope                                                         |
    |---------------------------------------------------------------|
    | `rpc://eth_*`                                                 |
    | `private://JPM_ACC1/_/contracts?owned.eoa=0x0&from.tm=JPM_K1` |
    | `private://Wallet1/_/contracts?owned.eoa=0x0&from.tm=JPM_K1`  |
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
    | `private://GS_ACC1/_/contracts?owned.eoa=0x0&from.tm=GS_K1` |
    | `private://Wallet2/_/contracts?owned.eoa=0x0&from.tm=GS_K1` |
* Configure `"GS Settlement"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
    | `private://GS_ACC2/_/contracts?owned.eoa=0x0&from.tm=GS_K2` |
    | `private://Wallet2/_/contracts?owned.eoa=0x0&from.tm=GS_K2` |
* Configure `"GS Research"` in authorization server with:
    | scope                                                       |
    |-------------------------------------------------------------|
    | `rpc://eth_*`                                               |
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
    | `private://DB_ACC1/_/contracts?owned.eoa=0x0&from.tm=DB_K1` |
    | `private://Wallet8/_/contracts?owned.eoa=0x0&from.tm=DB_K1` |

## JPM using self-managed account keys can only deploy private contracts from the allocated TM keys

tags: private, deploy, raw

 JPM who self-manages account keys use raw transaction flow to submit private transactions

* `"JPM Investment"` can deploy a "SimpleStorage" private contract to `"Node1"`, private from `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"`, private from `"GS_K1"`, signed by `"Wallet1"` and private for `"GS_K2"`

## JPM using self-managed account keys can only access to private contracts which are privy to it

tags: private, access, raw

 JPM who self-manages account keys use raw transaction flow to submit private transactions
 GS can not write to private contracts on which GS keys are not participants
 GS fails when attempting to use its private contract that manipulates other private contracts on which GS keys are not participants

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` can read "contract1" from "Node1"
* `"GS Investment"` fails to read "contract1" from "Node1"
* `"GS Investment"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`

## JPM using self-managed account keys can only receive events from private contracts which are privy to it

tags: private, events, raw

 JPM who self-manages account keys use raw transaction flow to submit private transactions
 JPM and GS can access to events via the following methods:

 - Call `eth_getTransactionReceipt` API

 - Call `eth_getLogs` API

 - Create a filter via `eth_newFilter` and call `eth_getFilterLogs`

 - Use Pub/Sub API for `logs` subscription (TBD)

 In this scenario, `DB Investment` is not one of the participants hence it is not able to receive log events despite using any of the above methods

* `"JPM Investment"` deploys a "ClientReceipt" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K2, GS_K2"`
* `"DB Investment"`, initially allocated with key(s) `"DB_K1"`, subscribes to log events from "contract1" in `"Node2"`, named this subscription as "subscriptionDB"
* `"JPM Settlement"`, initially allocated with key(s) `"JPM_K2"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionJPM"
* `"JPM Investment"` executes "contract1"'s `deposit()` function "5" times with arbitrary id and value between original parties
* "subscriptionJPM" receives "5" events
* `"JPM Settlement"`, initially allocated with key(s) `"JPM_K2"`, sees "5" events in total from transaction receipts in `"Node1"`
* `"GS Settlement"`, initially allocated with key(s) `"GS_K2"`, sees "5" events when filtering by "contract1" address in `"Node2"`
* "subscriptionDB" receives "0" events
* `"DB Investment"`, initially allocated with key(s) `"DB_K1"`, sees "0" events in total from transaction receipts in `"Node2"`
* `"DB Investment"`, initially allocated with key(s) `"DB_K1"`, sees "0" events when filtering by "contract1" address in `"Node2"`

## JPM using node-managed account keys can only deploy private contracts from the allocated TM keys

tags: private, deploy, node-managed-account

* `"JPM Investment"` can deploy a "SimpleStorage" private contract to `"Node1"` using `"JPM_ACC1"`, private from `"JPM_K1"` and private for `"JPM_K1"`
* `"JPM Investment"` can __NOT__ deploy a "SimpleStorage" private contract to `"Node1"` using `"JPM_ACC1"`, private from `"JPM_K2"` and private for `"JPM_K2"`

## JPM using node-managed account keys can only access to private contracts which are privy to it

tags: private, access, node-managed-account

 GS can not write to private contracts on which GS does not participate
 GS fails when attempting to use its private contract that manipulates other private contracts on which GS does not participate

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"JPM Investment"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"JPM Investment"` can read "contract1" from "Node1"
* `"GS Investment"` fails to read "contract1" from "Node1"
* `"GS Investment"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"GS_K1"`
* `"GS Investment"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"GS_K1"`
* `"GS Investment"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"GS_K1"`


## GS using node-managed account keys can only access events for contracts which are privy to it

tags: private, access, node-managed-account, events

 GS can not read transaction receipts for contracts in which it does not participate
 GS will only see events for the contracts it has access to (even though the transaction affects multiple contracts)

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"GS Investment"`, initially allocated with key(s) `"GS_K1"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionGS1"
* `"JPM Investment"`, initially allocated with key(s) `"JPM_K1"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionJPM1"
* `"JPM Investment"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"JPM Investment"` can read "contract1" from "Node1"
* `"GS Investment"` fails to read "contract1" from "Node1"
* `"JPM Investment"` checks the last arbitrary write transaction on `"Node1"` has `"1"` events
* `"GS Investment"` checks the last arbitrary write transaction on `"Node1"` has `"0"` events
* "subscriptionJPM1" receives "1" events
* "subscriptionGS1" receives "0" events
* `"GS Investment"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract1", by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"GS_K1,JPM_K1"`
* `"GS Investment"`, initially allocated with key(s) `"GS_K1"`, subscribes to log events from "delegateContract1" in `"Node1"`, named this subscription as "subscriptionGS2"
* `"JPM Investment"`, initially allocated with key(s) `"JPM_K1"`, subscribes to log events from "delegateContract1" in `"Node1"`, named this subscription as "subscriptionJPM2"
* `"JPM Investment"` writes a new arbitrary value to "delegateContract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* "subscriptionJPM1" receives "2" events
* "subscriptionGS1" receives "0" events
* "subscriptionJPM2" receives "1" events
* "subscriptionGS2" receives "1" events
* `"JPM Investment"` checks the last arbitrary write transaction on `"Node1"` has `"2"` events
* `"GS Investment"` checks the last arbitrary write transaction on `"Node1"` has `"1"` events
  JPM Settlement does not have access to either delegateContract1 or contract1 so it will not retrieve any events
* `"JPM Settlement"` checks the last arbitrary write transaction on `"Node1"` has `"0"` events

## JPM using node-managed account keys can only access events for contracts which are privy to it

tags: private, access, node-managed-account, events

 JPM Investment uses a delegate contract to create an event in both parent and child contracts.
 GS Investment should see only

Private contract with events between 2 parties: JPM Investment and GS Investment
* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1,GS_K1"`
* `"GS Investment"`, initially allocated with key(s) `"GS_K1"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionGS1"
* `"JPM Investment"`, initially allocated with key(s) `"JPM_K1"`, subscribes to log events from "contract1" in `"Node1"`, named this subscription as "subscriptionJPM1"
* `"JPM Investment"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1,GS_K1"`
* `"JPM Investment"` can read "contract1" from "Node1"
* `"GS Investment"` can read "contract1" from "Node1"
* "subscriptionJPM1" receives "1" events
* "subscriptionGS1" receives "1" events
* `"JPM Investment"` checks the last arbitrary write transaction on `"Node1"` has `"1"` events
* `"GS Investment"` checks the last arbitrary write transaction on `"Node1"` has `"1"` events
JPM Investment now uses a delegate contract (not for GS Investmment) to create 2 events: one from delegate contract, one from the above private contract (with GS Investment)
* `"JPM Investment"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"GS Investment"`, initially allocated with key(s) `"GS_K1"`, subscribes to log events from "delegateContract1" in `"Node1"`, named this subscription as "subscriptionGS2"
* `"JPM Investment"`, initially allocated with key(s) `"JPM_K1"`, subscribes to log events from "delegateContract1" in `"Node1"`, named this subscription as "subscriptionJPM2"
* `"JPM Investment"` writes a new arbitrary value to "delegateContract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
Parties to original private contracts would receive 2 events
* "subscriptionJPM1" receives "2" events
* "subscriptionGS1" receives "2" events
Non-party would not receive any
* "subscriptionJPM2" receives "1" events
* "subscriptionGS2" receives "0" events
Both would see transaction receipts but with different events count
* `"JPM Investment"` checks the last arbitrary write transaction on `"Node1"` has `"2"` events
* `"GS Investment"` checks the last arbitrary write transaction on `"Node1"` has `"1"` events

## JPM, GS and DB using self-managed account keys can access public states

tags: public, raw

* `"JPM Investment"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"`
* `"GS Investment"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node1"`
* `"DB Investment"` reads the value from "contract1" successfully by sending a request to `"Node2"` and the value returns "123"

## JPM, GS and DB using node-managed account keys can access public states

tags: public, node-managed-account

* `"JPM Investment"` deploys a "SimpleStorage" public contract, named "contract1", by sending a transaction to `"Node1"` using `"JPM_ACC1"`
* `"GS Settlement"` writes a new value "123" to "contract1" successfully by sending a transaction to `"Node2"` using `"GS_ACC2"`
* `"DB Investment"` reads the value from "contract1" successfully by sending a request to `"Node2"` and the value returns "123"

## GS using self-managed account keys can NOT bypass access controls to private contracts which are not privy to it by using nonce shifts

tags: private, access, raw

 Tenants who self-manage account keys use raw transaction flow to submit private transactions
 `GS Investment` can not write to private contracts on which `B1` or `B2` are not participants
 `GS Investment` fails when attempting to use its private contract that manipulates other private contracts on which `B1` or `B2` are not participants

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "contract1", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` writes a new arbitrary value to "contract1" successfully by sending a transaction to `"Node1"` with its TM key `"JPM_K1"`, signed by `"Wallet1"` and private for `"JPM_K1"`
* `"JPM Investment"` can read "contract1" from "Node1"
* `"GS Investment"` fails to read "contract1" from "Node1"
* `"GS Investment"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` fails to write a new arbitrary value to "contract1" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"JPM_K1"`
* `"GS Investment"` deploys a "SimpleStorageDelegate(contract1)" private contract, named "delegateContract", by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` fails to write a new arbitrary value to "delegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
 GS Investment sends two transactions (the first one has a nonce higer than the second):
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
* `"GS Investment"` deploys a "SneakyWrapper(contract1)" private contract, named "sneakyDelegateContract", by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` invokes getFromDelgate with nonce shift "1" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"` name this transaction "sneakyDelegateContract_TX1"
* `"GS Investment"` invokes setDelegate to "true" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"GS_K1"`
* `"GS Investment"` checks the transaction "sneakyDelegateContract_TX1" on `"Node1"` has failed
 After adding post execution checks the SneakyWrapper is unable to copy the data from contract1
* `"GS Investment"` invokes get in "sneakyDelegateContract" on `"Node1"` and gets value "0"

 Using a proxy contract and privateFor the target party in order to manipulate the state

* `"GS Investment"` deploys a "SneakyWrapper(contract1)" private contract, named "sneakyDelegateContract", by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"JPM_K1"`
* `"GS Investment"` invokes getFromDelgate with nonce shift "1" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"JPM_K1"` name this transaction "sneakyDelegateContract_TX1"
* `"GS Investment"` invokes setDelegate to "true" in "sneakyDelegateContract" by sending a transaction to `"Node1"` with its TM key `"GS_K1"`, signed by `"Wallet2"` and private for `"JPM_K1"`
* `"GS Investment"` checks the transaction "sneakyDelegateContract_TX1" on `"Node1"` has failed
 After adding post execution checks the SneakyWrapper is unable to copy the data from contract1
* `"GS Investment"` invokes get in "sneakyDelegateContract" on `"Node1"` and gets value "0"

## JPM can access its contract code

tags: private, opcodes

This is a low level scenario in which a client tries too read contract code.

* `"JPM Investment"` deploys a "ContractCodeReader" private contract, named "reader", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"JPM Investment"` invokes ContractCodeReader("reader")'s getCodeSize("reader") on `"Node1"` and gets non-empty value
* `"JPM Investment"` invokes ContractCodeReader("reader")'s getCode("reader") on `"Node1"` and gets non-empty value
* `"JPM Investment"` invokes ContractCodeReader("reader")'s getCodeHash("reader") on `"Node1"` and gets non-empty value
* `"JPM Investment"` invokes ContractCodeReader("reader")'s setLastCodeSize("reader") by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"JPM Investment"` invokes ContractCodeReader("reader")'s getLastCodeSize() on `"Node1"` and gets non-empty value

## GS can NOT access non-authorized contract code

tags: private, opcodes

This is a low level scenario in which a client tries too read un-authorized contract code

* `"JPM Investment"` deploys a "SimpleStorage" private contract, named "target", by sending a transaction to `"Node1"` with its TM key `"JPM_K1"` using `"JPM_ACC1"` and private for `"JPM_K1"`
* `"GS Investment"` deploys a "ContractCodeReader" private contract, named "reader", by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"JPM_K1"`
* `"GS Investment"` fails to invoke ContractCodeReader("reader")'s setLastCodeSize("target") by sending a transaction to `"Node1"` with its TM key `"GS_K1"` using `"GS_ACC1"` and private for `"JPM_K1"`
* `"GS Investment"` invokes ContractCodeReader("reader")'s getLastCodeSize() on `"Node1"` and gets empty value
* `"GS Investment"` invokes ContractCodeReader("reader")'s getCodeSize("target") on `"Node1"` and gets "not authorized"
* `"GS Investment"` invokes ContractCodeReader("reader")'s getCode("target") on `"Node1"` and gets "not authorized"
* `"GS Investment"` invokes ContractCodeReader("reader")'s getCodeHash("target") on `"Node1"` and gets "not authorized"
