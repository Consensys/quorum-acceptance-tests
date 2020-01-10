# Extend contract to new party

 Tags: extension

This is to verify that a node must not be able to send transactions to private smart contract which it is not party to.

There are 2 set of contracts to be used selectively in the below scenarios.

Single smart contract
```
pragma solidity ^0.5.0;

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
pragma solidity ^0.5.0;

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

pragma solidity ^0.5.0;

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
## Extend a contract to a new party - happy path

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract1Extension"
* "contract1Extension" is deployed "successfully" in "Node2,Node4"

* "contract1Extension"'s `get()` function execution in "Node2" returns "42"
* "contract1Extension"'s `get()` function execution in "Node4" returns "42"
* "contract1Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract1Extension"
* "Node1" accepts the offer to extend the contract "contract1Extension"
* "Node4" votes "true" to extending contract "contract1Extension"
* Wait for "contract1Extension" to disappear from active extension in "Node1"
* "contract1Extension"'s `get()` function execution in "Node1" returns "42"

## Extend contract but new party doesn't accept

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract2Extension"
* "contract2Extension" is deployed "successfully" in "Node2,Node4"
* "contract2Extension"'s `get()` function execution in "Node2" returns "42"
* "contract2Extension"'s `get()` function execution in "Node4" returns "42"
* "contract2Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract2Extension"
* "Node4" votes "true" to extending contract "contract2Extension"
* "Node1" has "contract2Extension" listed in all active extensions
* "Node1" votes "false" to extending contract "contract2Extension"
* Wait for "contract2Extension" to disappear from active extension in "Node1"
* "contract2Extension"'s `get()` function execution in "Node1" returns "0"
* "Node2" does not see "contract2Extension" listed in all active extensions

## Voter votes to not extend

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract3Extension"
* "contract3Extension" is deployed "successfully" in "Node2,Node4"
* "contract3Extension"'s `get()` function execution in "Node2" returns "42"
* "contract3Extension"'s `get()` function execution in "Node4" returns "42"
* "contract3Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract3Extension"
* "Node1" has "contract3Extension" listed in all active extensions
* "Node4" votes "false" to extending contract "contract3Extension"
* "Node1" does not see "contract3Extension" listed in all active extensions
* Wait for "contract3Extension" to disappear from active extension in "Node1"
* "contract3Extension"'s `get()` function execution in "Node1" returns "0"
* "Node2" does not see "contract3Extension" listed in all active extensions

## Creator cancels contract after creating extension request
* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract4Extension"
* "contract4Extension" is deployed "successfully" in "Node2,Node4"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract4Extension"

* "Node1" has "contract4Extension" listed in all active extensions
* "Node2" cancels "contract4Extension"
* Wait for "contract4Extension" to disappear from active extension in "Node1"
* "Node1" does not see "contract4Extension" listed in all active extensions

## Extend a contract to a new party - extending contract after initial state change

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract5Extension"
* "contract5Extension" is deployed "successfully" in "Node2,Node4"
* "contract5Extension"'s `get()` function execution in "Node2" returns "42"
* "contract5Extension"'s `get()` function execution in "Node4" returns "42"
* "contract5Extension"'s `get()` function execution in "Node1" returns "0"

* Execute "contract5Extension"'s `set()` function with new value "129" in "Node2" and it's private for "Node4"
* "contract5Extension"'s `get()` function execution in "Node2" returns "129"
* "contract5Extension"'s `get()` function execution in "Node4" returns "129"
* "contract5Extension"'s `get()` function execution in "Node1" returns "0"

* Execute "contract5Extension"'s `set()` function with new value "999" in "Node2" and it's private for "Node4"
* "contract5Extension"'s `get()` function execution in "Node2" returns "999"
* "contract5Extension"'s `get()` function execution in "Node4" returns "999"
* "contract5Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract5Extension"
* "Node1" accepts the offer to extend the contract "contract5Extension"
* "Node4" votes "true" to extending contract "contract5Extension"
* Wait for "contract5Extension" to disappear from active extension in "Node1"

* "contract5Extension"'s `get()` function execution in "Node2" returns "999"
* "contract5Extension"'s `get()` function execution in "Node4" returns "999"
* "contract5Extension"'s `get()` function execution in "Node1" returns "999"

## Extend a contract to a new party - state corruption for initiator when recepient voter account is not given
* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract6Extension"
* "contract6Extension" is deployed "successfully" in "Node2,Node4"
* "contract6Extension"'s `get()` function execution in "Node2" returns "42"
* "contract6Extension"'s `get()` function execution in "Node4" returns "42"
* "contract6Extension"'s `get()` function execution in "Node1" returns "0"

* Execute "contract6Extension"'s `set()` function with new value "99" in "Node2" and it's private for "Node4"
* "contract6Extension"'s `get()` function execution in "Node2" returns "99"
* "contract6Extension"'s `get()` function execution in "Node4" returns "99"
* "contract6Extension"'s `get()` function execution in "Node1" returns "0"

* Execute "contract6Extension"'s `set()` function with new value "999" in "Node2" and it's private for "Node4"
* "contract6Extension"'s `get()` function execution in "Node2" returns "999"
* "contract6Extension"'s `get()` function execution in "Node4" returns "999"
* "contract6Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with only "Node2,Node4" as voters for contract "contract6Extension"
* "Node4" votes "true" to extending contract "contract6Extension"
* Wait for "contract6Extension" to disappear from active extension in "Node1"
* "contract6Extension"'s `get()` function execution in "Node2" returns "999"
* "contract6Extension"'s `get()` function execution in "Node4" returns "999"
* "contract6Extension"'s `get()` function execution in "Node1" returns "0"

## Re-extend an alraedy extende contract - state change should not happen
* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract7Extension"
* "contract7Extension" is deployed "successfully" in "Node2,Node4"
* "contract7Extension"'s `get()` function execution in "Node2" returns "42"
* "contract7Extension"'s `get()` function execution in "Node4" returns "42"
* "contract7Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract7Extension"
* "Node1" accepts the offer to extend the contract "contract7Extension"
* "Node4" votes "true" to extending contract "contract7Extension"
* Wait for "contract7Extension" to disappear from active extension in "Node1"
* "contract7Extension"'s `get()` function execution in "Node2" returns "42"
* "contract7Extension"'s `get()` function execution in "Node4" returns "42"
* "contract7Extension"'s `get()` function execution in "Node1" returns "42"

* Execute "contract7Extension"'s `set()` function with new value "999" in "Node2" and it's private for "Node4"
* "contract7Extension"'s `get()` function execution in "Node2" returns "999"
* "contract7Extension"'s `get()` function execution in "Node4" returns "999"
* "contract7Extension"'s `get()` function execution in "Node1" returns "42"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract7Extension"
* "Node1" accepts the offer to extend the contract "contract7Extension"
* "Node4" votes "true" to extending contract "contract7Extension"
* Wait for "contract7Extension" to disappear from active extension in "Node1"
* "contract7Extension"'s `get()` function execution in "Node2" returns "999"
* "contract7Extension"'s `get()` function execution in "Node4" returns "999"
* "contract7Extension"'s `get()` function execution in "Node1" returns "42"

