# Public raw smart contract when signed externally

 Tags: basic, raw, externally-signed, public

A smart contract, `ClientReceipt`, logs all the deposits that have been performed.
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

    function get() public view returns (uint retVal) {
        return storedData;
    }
}
```

## Contract is deployed and mined

* Deploy `SimpleStorage` public smart contract with initial value "32" signed by external wallet "Wallet1" on node "Node2", name this contract as "contract32"
* "contract32" is mined

## Everyone in the network has the same state for the contract

 Tags: raw

* "contract32"'s `get()` function execution in "Node1" returns "32"
* "contract32"'s `get()` function execution in "Node2" returns "32"
* "contract32"'s `get()` function execution in "Node4" returns "32"
* "contract32"'s `get()` function execution in "Node3" returns "32"

## When there's an update, every node is updated

 Tags: raw

* Execute "contract32"'s `set()` function with new value "5" signed by external wallet "Wallet8" in "Node3"
* "contract32"'s `get()` function execution in "Node1" returns "5"
* "contract32"'s `get()` function execution in "Node2" returns "5"
* "contract32"'s `get()` function execution in "Node4" returns "5"
* "contract32"'s `get()` function execution in "Node3" returns "5"
