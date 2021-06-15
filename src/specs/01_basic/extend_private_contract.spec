# Extend contract to new party

 Tags: basic, extension

This is to verify the contract extension APIs (success scenarios).

There are 2 set of contracts to be used selectively in the below scenarios.

Single smart contract
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

Nested smart contracts
```
pragma solidity ^0.5.17;

import "./C2.sol";

contract C1 {

    uint x;

    constructor(uint initVal) public {
        x = initVal;
    }

    function set(uint newValue) public returns (uint) {
        x = newValue;
        return x;
    }

    function get() public view returns (uint) {
        return x;
    }

    function newContractC2(uint newValue) public {
        C2 c = new C2(address(this));
        // method calling C1 set function
        c.set(newValue);
    }
}

pragma solidity ^0.5.17;

import "./C1.sol";

contract C2 {

    C1 c1;
    uint y;

    constructor(address _t) public {
        c1 = C1(_t);
    }

    function get() public view returns (uint result) {
        return c1.get();
    }

    function set(uint _val) public {
        y = _val;
        c1.set(_val);
    }

    function restoreFromC1() public {
        y = c1.get();
    }
}

```
  This spec will be executed for all 3 privacy flag values (PartyProtection and StateValidation in a separate privacy enhanced spec)

      |privacyType      |
      |StandardPrivate  |



## Extend a contract to a new party, new party accepts the extension
* Deploy <privacyType> type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract1Extension". Ensure that its not visible from "Node1"
* Initiate "contract1Extension" extension from "Node2" to "Node1". Contract extension accepted in receiving node. Check that state value in receiving node is "42"

## Extend contract to a new party, new party rejects the extension
* Deploy <privacyType> type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract2Extension". Ensure that its not visible from "Node1"
* Initiate "contract2Extension" extension from "Node2" to "Node1". Contract extension is rejected by receiving node. Check that state value in receiving node is "0"

## Extend contract to a new party, creator cancels extension
* Deploy <privacyType> type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract3Extension". Ensure that its not visible from "Node1"
* Initiate "contract3Extension" extension from "Node2" to "Node1". Contract extension cancelled by initiating node. Confirm that contract extension is not visible on receiving node

## Extend a contract to a new party - extending contract after initial state change
* Deploy <privacyType> type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract4Extension". Ensure that its not visible from "Node1"

* Execute "contract4Extension"'s `set()` function with privacy type <privacyType> to set new value to "129" in "Node2" and it's private for "Node4"
* "contract4Extension"'s `get()` function execution in "Node2" returns "129"
* "contract4Extension"'s `get()` function execution in "Node4" returns "129"
* "contract4Extension"'s `get()` function execution in "Node1" returns "0"

* Execute "contract4Extension"'s `set()` function with privacy type <privacyType> to set new value to "999" in "Node2" and it's private for "Node4"
* "contract4Extension"'s `get()` function execution in "Node2" returns "999"
* "contract4Extension"'s `get()` function execution in "Node4" returns "999"
* "contract4Extension"'s `get()` function execution in "Node1" returns "0"

* Initiate "contract4Extension" extension from "Node2" to "Node1". Contract extension accepted in receiving node. Check that state value in receiving node is "999"

## Extend a non-existent private contract - should fail
* Deploy <privacyType> type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract6Extension". Ensure that its not visible from "Node1"

* Initiating contract extension to "Node1" with its default account as recipient from "Node3" for contract "contract6Extension" should fail with error "extending a non-existent private contract!!! not allowed"

## Extend nested contracts and check state once all contracts are extended - state should be visible
* Deploy <privacyType> "storec" smart contract with initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "c2"
* "c2"'s "getc" function execution in "Node1" returns "1"
* Deploy <privacyType> "storeb" smart contract with contract "c2" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "b2"
* "b2"'s "getb" function execution in "Node1" returns "1"
* Deploy <privacyType> "storea" smart contract with contract "b2" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "a2"
* "a2"'s "geta" function execution in "Node1" returns "1"
* "a2"'s "setc" function execution with privacy flag as <privacyType> in "Node1" with value "10" and its private for "Node2"
* "a2"'s "getc" function execution in "Node1" returns "10"
* "a2"'s "setb" function execution with privacy flag as <privacyType> in "Node1" with value "10" and its private for "Node2"
* "a2"'s "getb" function execution in "Node1" returns "100"
* "a2"'s "seta" function execution with privacy flag as <privacyType> in "Node1" with value "10" and its private for "Node2"
* "a2"'s "geta" function execution in "Node1" returns "1000"
* "b2"'s "getb" function execution in "Node1" returns "100"
* "c2"'s "getc" function execution in "Node1" returns "10"

* Initiate contract extension to "Node4" with its default account as recipient from "Node1" for contract "c2"
* "Node4" accepts the offer to extend the contract "c2"
* Wait for "c2" to disappear from active extension in "Node1"

* Initiate contract extension to "Node4" with its default account as recipient from "Node1" for contract "b2"
* "Node4" accepts the offer to extend the contract "b2"
* Wait for "b2" to disappear from active extension in "Node1"

* Initiate contract extension to "Node4" with its default account as recipient from "Node1" for contract "a2"
* "Node4" accepts the offer to extend the contract "a2"
* Wait for "a2" to disappear from active extension in "Node1"

* "c2"'s "getc" function execution in "Node4" returns "10"
* "b2"'s "getb" function execution in "Node4" returns "100"
* "a2"'s "geta" function execution in "Node4" returns "1000"

