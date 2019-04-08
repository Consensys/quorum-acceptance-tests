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
* Fail to execute "PartyProtection" simple contract("contract1Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

* "contract1Extension"'s `get()` function execution in "Node2" returns "42"
* "contract1Extension"'s `get()` function execution in "Node4" returns "42"
* "contract1Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract1Extension"
* "Node1" accepts the offer to extend the contract "contract1Extension"
* "Node4" votes "true" to extending contract "contract1Extension"
* "Node2" votes "true" to extending contract "contract1Extension"

* "contract1Extension"'s `get()` function execution in "Node1" returns "42"
* Fail to execute "PartyProtection" simple contract("contract1Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

* "Node2" requests parties are updated for contract "contract1Extension"
* Fire and forget execution of "PartyProtection" simple contract("contract1Extension")'s `set()` function with value "86" in "Node1" and it's private for "Node4"
* "contract1Extension"'s `get()` function execution in "Node4" returns "86"
* "contract1Extension"'s `get()` function execution in "Node1" returns "86"

## Extend contract but new party doesn't accept

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract2Extension"
* "contract2Extension" is deployed "successfully" in "Node2,Node4"
* Fail to execute "PartyProtection" simple contract("contract2Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
* "contract2Extension"'s `get()` function execution in "Node2" returns "42"
* "contract2Extension"'s `get()` function execution in "Node4" returns "42"
* "contract2Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract2Extension"
* "Node4" votes "true" to extending contract "contract2Extension"
* "Node2" votes "true" to extending contract "contract2Extension"

* "contract2Extension"'s `get()` function execution in "Node1" returns "0"
* Fail to execute "PartyProtection" simple contract("contract2Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

* "Node2" requests parties are updated for contract "contract2Extension"
* Fail to execute "PartyProtection" simple contract("contract2Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

## Voter votes to not extend

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract3Extension"
* "contract3Extension" is deployed "successfully" in "Node2,Node4"
* Fail to execute "PartyProtection" simple contract("contract3Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
* "contract3Extension"'s `get()` function execution in "Node2" returns "42"
* "contract3Extension"'s `get()` function execution in "Node4" returns "42"
* "contract3Extension"'s `get()` function execution in "Node1" returns "0"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract3Extension"

* "Node1" has "contract3Extension" listed in all active extensions

* "Node4" votes "false" to extending contract "contract3Extension"
* "Node2" votes "true" to extending contract "contract3Extension"

* "contract3Extension"'s `get()` function execution in "Node1" returns "0"
* Fail to execute "PartyProtection" simple contract("contract3Extension")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

* "Node2" sees "contract3Extension" has status "DECLINED"
* "Node2" cancels "contract3Extension"
* "Node2" does not see "contract3Extension" listed in all active extensions

## Creator cancels contract after creating extension request
* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node2"'s default account and it's private for "Node4", named this contract as "contract4Extension"
* "contract4Extension" is deployed "successfully" in "Node2,Node4"

* Create a "PartyProtection" extension to "Node1" from "Node2" with "Node2,Node4" as voters for contract "contract4Extension"

* "Node1" has "contract4Extension" listed in all active extensions
* "Node2" cancels "contract4Extension"
* "Node1" does not see "contract4Extension" listed in all active extensions
