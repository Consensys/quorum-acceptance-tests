# GraphQL query privacy precompile private transaction

  Tags: graphql, privacy-precompile-enabled

The following smart contract is used:
```
pragma solidity ^0.5.17;

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

## Get privacy precompile private transaction details from graphql query

  Tags: private, smart-contract

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node1" and it should equal to "false"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node2" and it should equal to "false"
* Get isPrivate field for "contract17"'s contract deployment transaction using GraphQL query from "Node4" and it should equal to "false"
* Get isPrivate field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node1" and it should equal to "true"
* Get isPrivate field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node2" and it should equal to "null"
* Get isPrivate field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node4" and it should equal to "true"
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node1" and it should be equal to "0x"
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node2" and it should be equal to "0x"
* Get privateInputData field for "contract17"'s contract deployment transaction using GraphQL query from "Node4" and it should be equal to "0x"
* Get privateInputData field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node1" and it should be the same as eth_getQuorumPayload
* Get privateInputData field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node2" and it should be equal to "null"
* Get privateInputData field for "contract17"'s contract deployment internal transaction using GraphQL query from "Node4" and it should be the same as eth_getQuorumPayload
