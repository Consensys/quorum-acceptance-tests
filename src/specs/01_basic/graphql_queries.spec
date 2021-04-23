# GraphQL query block and private transaction

  Tags: basic, graphql

The following smart contract is used:
```
pragma solidity ^0.4.15;

contract SimpleStorage {
    uint private storedData;

    constructor(uint initVal) public {
        storedData = initVal;
    }

    function set(uint x) public {
        storedData = x;
    }

    function get() public constant returns (uint retVal) {
        return storedData;
    }
}
```

## Get block number from graphql query and compare it with RPC result

* Capture the current block height, named it as "snapshot"
* Get block number from "Node1" graphql and it should be greater than or equal to "snapshot"

## Get private transaction details from graphql query

  Tags: private, smart-contract

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node1" and it should equal to "true"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node2" and it should equal to "true"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node4" and it should equal to "true"
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node1" and it should be the same as eth_getQuorumPayload
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node2" and it should be the same as eth_getQuorumPayload
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node4" and it should be the same as eth_getQuorumPayload
