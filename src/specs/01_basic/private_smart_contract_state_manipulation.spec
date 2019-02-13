# State manipulation in private smart contract

 Tags: basic

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
Non-participated party from original transaction is trying to modify the private state of the participated party.

* Note: this step is not implemented indicating that the spec execution is temporarily skipped
* Deploy a simple smart contract with initial value "100" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"

## Private state is maintained

 Tags: private, state

* Execute "contract17"'s `set()` function with new value "0" in "Node3" and it's private for "Node1"
* "contract17"'s `get()` function execution in "Node1" returns "100"
