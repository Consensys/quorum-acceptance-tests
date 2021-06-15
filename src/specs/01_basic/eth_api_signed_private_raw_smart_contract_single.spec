# Private raw smart contract when signed using the eth_signTransaction API

 Tags: basic, raw, eth-api-signed, private

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

* Deploy a simple smart contract with initial value "23" signed with "eth_signTransaction" using "Node1"'s default account and it's private for "Node4", name this contract as "contract31"
* Transaction Hash is returned for API signed "contract31"
* Transaction Receipt is present in "Node1" for eth_signTransaction signed "contract31" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for eth_signTransaction signed "contract31" from "Node1"'s default account

## Storage Root storing private smart contracts must be the same

 Tags: raw, storage-root

* "contract31" stored in "Node1" and "Node4" must have the same storage root
* "contract31" stored in "Node1" and "Node3" must not have the same storage root

## Transaction payload is secured

 Tags: raw, get-quorum-payload

* "contract31"'s payload is retrievable from "Node1"
* "contract31"'s payload is retrievable from "Node4"
* "contract31"'s payload is not retrievable from "Node3"

## Privacy is enforced between parties

 Tags: raw

* "contract31"'s `get()` function execution in "Node1" returns "23"
* "contract31"'s `get()` function execution in "Node4" returns "23"
* "contract31"'s `get()` function execution in "Node3" returns "0"

## When there's an update, privacy is still enforced

 Tags: raw

* Execute "contract31"'s `set()` function with new value "5" signed with "eth_signTransaction" using "Node1"'s default account and it's private for "Node4"
* "contract31"'s `get()` function execution in "Node1" returns "5"
* "contract31"'s `get()` function execution in "Node4" returns "5"
* "contract31"'s `get()` function execution in "Node3" returns "0"
