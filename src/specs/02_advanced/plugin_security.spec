# Pluggable Architecture with security plugin

 Tags: networks/plugins::istanbul-rpc-security, networks/plugins::qbft-rpc-security, networks/plugins::raft-rpc-security, pre-condition/no-record-blocknumber

* Configure the authorization server to grant `"Client_1"` access to scopes `"rpc://eth_*,rpc://rpc_modules"` in `"Node1,Node2"`
* Configure the authorization server to grant `"Operator_A"` access to scopes `"rpc://admin_peers"` in `"Node1,Node2"`
* Configure the authorization server to grant `"Client_3"` access to scopes `"rpc://graphql_*"` in `"Node1,Node2"`

## Clients are authorized to access APIs based on specific scope granted

 Tags: specific-scope

* `"Client_1"` requests access token for scope(s) `"rpc://rpc_modules"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Client_1"` is responded with "success" when trying to:
    | callApi       | targetNode |
    |---------------|------------|
    | `rpc_modules` | `Node1`    |
    | `rpc_modules` | `Node2`    |
* `"Client_1"` is responded with "access denied" when trying to:
    | callApi                 | targetNode |
    |-------------------------|------------|
    | `admin_peers`           | `Node1`    |
    | `personal_listAccounts` | `Node2`    |
* `"Operator_A"` requests access token for scope(s) `"rpc://admin_peers"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Operator_A"` is responded with "success" when trying to:
    | callApi       | targetNode |
    |---------------|------------|
    | `admin_peers` | `Node1`    |
    | `admin_peers` | `Node2`    |
* `"Operator_A"` is responded with "access denied" when trying to:
    | callApi           | targetNode |
    |-------------------|------------|
    | `eth_blockNumber` | `Node1`    |
    | `eth_accounts`    | `Node2`    |

## Clients are authorized to access APIs based on wildcard scope granted

 Tags: wildcard-scope

Wildcard can be used to define access scope. `rpc://eth_*` means all APIs in `eth` namespace

* `"Client_1"` requests access token for scope(s) `"rpc://eth_*"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Client_1"` is responded with "success" when trying to:
    | callApi           | targetNode |
    |-------------------|------------|
    | `eth_blockNumber` | `Node1`    |
    | `eth_accounts`    | `Node2`    |
* `"Client_1"` is responded with "access denied" when trying to:
    | callApi                 | targetNode |
    |-------------------------|------------|
    | `personal_listAccounts` | `Node1`    |
    | `admin_datadir`         | `Node2`    |

## Clients are authorized to access APIs based on target nodes granted

 Tags: target-node

* `"Client_1"` requests access token for scope(s) `"rpc://eth_*"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Client_1"` is responded with "invalid audience claim (aud)" when trying to:
    | callApi           | targetNode |
    |-------------------|------------|
    | `eth_accounts`    | `Node3`    |
* `"Operator_A"` requests access token for scope(s) `"rpc://admin_peers"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Operator_A"` is responded with "invalid audience claim (aud)" when trying to:
    | callApi       | targetNode |
    |---------------|------------|
    | `admin_peers` | `Node4`    |

## Clients are authorized to access APIs in a batch call

 Tags: batch

JSON RPC allows clients to perform multiple APIs in a batch by sending an array of JSON RPC calls.
In this scenario, the assumption is that all APIs in the batch are authorized.

* `"Client_1"` requests access token for scope(s) `"rpc://eth_*,rpc://rpc_modules"` and audience(s) `"Node1"` from the authorization server
* `"Client_1"` is responded with "success" when trying to:
    | callApi                       | targetNode |
    |-------------------------------|------------|
    | `eth_blockNumber,rpc_modules` | `Node1`    |
* `"Client_1"` is responded with "access denied" when trying to:
    | callApi                               | targetNode |
    |---------------------------------------|------------|
    | `personal_listAccounts,admin_datadir` | `Node1`    |

## Clients are authorized to access some of APIs in a batch call

 Tags: batch

In this scenario, the assumption is that some of APIs in the batch are authorized.

* `"Client_1"` requests access token for scope(s) `"rpc://eth_*,rpc://rpc_modules"` and audience(s) `"Node1"` from the authorization server
* `"Client_1"` sends a batch `"eth_blockNumber,personal_listAccounts,rpc_modules,admin_datadir"` to `"Node1"` and expect:
    | callApi                 | expectation   |
    |-------------------------|---------------|
    | `eth_blockNumber`       | success       |
    | `personal_listAccounts` | access denied |
    | `rpc_modules`           | success       |
    | `admin_datadir`         | access denied |

## Clients are required to provice access token when calling APIs

 Tags: access-token

* `"Client_1"` doesn't request access token from the authorization server
* `"Client_1"` is responded with "missing access token" when trying to:
    | callApi       | targetNode |
    |---------------|------------|
    | `rpc_modules` | `Node1`    |
    | `rpc_modules` | `Node2`    |

## Authorized client sends private transactions

 Tags: basic-rpc-security

* Configure the authorization server to grant `"Client_1"` access to scopes `"rpc://eth_*"` in `"Node1,Node3,Node4"`
* `"Client_1"` requests access token for scope(s) `"rpc://eth_*"` and audience(s) `"Node1,Node3,Node4"` from the authorization server
* Setup `"Client_1"` access token for subsequent calls
* Verify privacy between "Node1" and "Node4" excluding "Node3" when using a simple smart contract

## Client graphql access

 Tags: basic-rpc-security

* `"Client_3"` requests access token for scope(s) `"rpc://graphql_*"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Client_3"` is responded with "success" when trying to access graphql on "Node1"
* `"Client_3"` is responded with "invalid audience claim (aud)" when trying to access graphql on "Node3"
* `"Operator_A"` requests access token for scope(s) `"rpc://admin_peers"` and audience(s) `"Node1,Node2"` from the authorization server
* `"Operator_A"` is responded with "access denied" when trying to access graphql on "Node1"
