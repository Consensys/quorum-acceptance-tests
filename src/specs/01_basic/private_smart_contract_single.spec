# Single private smart contract

 Tags: basic, single

This is to verify that a private smart contract between 2 parties are not accessible by others.
A simple smart contract is to store a int value and to provide `get()` and `set()` functions.
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

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"

## Contract is mined

 Tags: private, mining

* Transaction Hash is returned for "contract17"
* Transaction Receipt is present in "Node1" for "contract17" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for "contract17" from "Node1"'s default account

## Storage Root storing private smart contracts must be the same

 Tags: private, storage-root

* "contract17" stored in "Node1" and "Node4" must have the same storage root
* "contract17" stored in "Node1" and "Node3" must not have the same storage root

## Transaction payload is secured

 Tags: private, transaction, get-quorum-payload

* "contract17"'s payload is retrievable from "Node1"
* "contract17"'s payload is retrievable from "Node4"
* "contract17"'s payload is not retrievable from "Node3"

## Privacy is enforced between parties

 Tags: private

* "contract17"'s `get()` function execution in "Node1" returns "42"
* "contract17"'s `get()` function execution in "Node4" returns "42"
* "contract17"'s `get()` function execution in "Node3" returns "0"

## When there's an update, privacy is still enforced

 Tags: private, privacy

* Execute "contract17"'s `set()` function with new value "5" in "Node1" and it's private for "Node4"
* "contract17"'s `get()` function execution in "Node1" returns "5"
* "contract17"'s `get()` function execution in "Node4" returns "5"
* "contract17"'s `get()` function execution in "Node3" returns "0"
