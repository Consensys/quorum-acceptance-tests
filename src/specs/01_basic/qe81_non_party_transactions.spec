# Deny non-party sending private transactions to protected contracts

 Tags: privacy, non-party, privacy-enhancements, qe81

Private smart contract is protected by having party protection flag enabled during its creation.

This is to verify that a node must not be able to send transactions to private smart contract which it is not party to.

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

## Transactions sent to simple contract by non-party node without flag will result in mismatched receipts

 Tags: single

* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fire and forget execution of simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"
* Transaction Receipt is "unsuccessfully" available in "Node1" for "contract14"
* Transaction Receipt is "successfully" available in "Node3" for "contract14"
* Transaction Receipt is "successfully" available in "Node4" for "contract14"

## Privacy is maintained when non-party node trying to send a transaction to a simple contract without flag

 Tags: single

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fire and forget execution of simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"
* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"

## Deny transactions sent to simple contract by non-party node

 Tags: single, deny

* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "PartyProtection" simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1" with error "contract not found. cannot transact"

## Privacy is maintained when non-party node trying to send a transaction to a simple contract

 Tags: single

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "PartyProtection" simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1" with error "contract not found. cannot transact"
* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"

## Deny transactions sent to a nested contract by non-party node

 Tags: nested, deny

* Deploy a "PartyProtection" C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PartyProtection" C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute "PartyProtection" contract `C2`("childContractC2_12")'s `set()` function with new arbitrary value in "Node2" and it's private for "Node1" with error "JsonRpcError thrown with code -32000. Message: execution reverted"


## Privacy is maintained when non-party node trying to send a transaction to a nested contract

 Tags: nested

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

* Deploy a "PartyProtection" C1 contract with initial value "30" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PartyProtection" C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute "PartyProtection" contract `C2`("childContractC2_12")'s `set()` function with new arbitrary value in "Node2" and it's private for "Node1" with error "JsonRpcError thrown with code -32000. Message: execution reverted"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node1" returns "30"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node4" returns "30"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node2" returns "0"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node1" returns "30"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node4" returns "0"
* Fail to execute contract `C2`("childContractC2_12")'s `get()` function in "Node2" with error "execution reverted"

## Privacy is maintained when a transaction to a contract creating another contract then in turn referencing its creator

 Tags: nested-call, affected

This is to make sure we capture the affected contracts correctly.
In this scenario, contract C2 is created via `newContractC2()` call to C1, then invoking C2 function that eventually changes C1 value.

* Deploy a "PartyProtection" C1 contract with initial value "5" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Execute "PartyProtection" "contractC1_14"'s `newContractC2()` function with new value "100" in "Node1" and it's private for "Node4"
* Contract `C1`("contractC1_14")'s `get()` function execution in "Node1" returns "100"
* Contract `C1`("contractC1_14")'s `get()` function execution in "Node4" returns "100"

## [Limitation] Transaction sent by a node which is party to parent and child contracts would be successful regardless the privateFor

 Tags: limitation, transaction

As Node1 is party to both C1 and C2, transaction to C2 sent by Node1 (privateFor Node2) will produce successful receipt hence modify C1's state.
Transaction receipt in Node2 will be marked as failure.

* Deploy a "PartyProtection" C1 contract with initial value "30" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PartyProtection" C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Fire and forget execution of "PartyProtection" contract `C2`("childContractC2_12")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
* Transaction Receipt is "successfully" available in "Node1" for "childContractC2_12"
* Transaction Receipt is "unsuccessfully" available in "Node2" for "childContractC2_12"

## [Limitation] Read-only call sent by a node which is party to parent and child contracts would be successful

 Tags: limitation, read-only

As Node1 is party to both C1 and C2, a read-only call to C2 sent by Node1 will return result.

* Deploy a "PartyProtection" C1 contract with initial value "123" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PartyProtection" C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node1" returns "123"
