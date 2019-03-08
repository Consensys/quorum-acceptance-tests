# Single private smart contract - non-party sending transaction

 Tags: basic, privacy, non-party, privacy-enhancements

This is to verify that a node must not be able to send transactions to private smart contract to which it is not party.

A simple smart contract is to store a int value and to provide `get()` and `set()` functions.
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

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"

## Contract is mined

 Tags: private, mining

* Transaction Hash is returned for "contract14"
* Transaction Receipt is present in "Node1" for "contract14"
* Transaction Receipt is present in "Node4" for "contract14"

## Privacy is enforced between parties

 Tags: private

* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"

## Privacy is maintained when non-party node trying to send a transaction to the contract

 Tags: private

* Fail to execute "contract14"'s `set()` function with new value "5" in "Node3" and it's private for "Node1"
* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"
