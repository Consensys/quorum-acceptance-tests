# Deny non-party sending private transactions

tags: basic, non-party, privacy, privacy-enhancements, qe81

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

contract C1 {

   uint x;

   function set(uint newValue) public returns (uint) {
       x = newValue;
       return x;
   }

   function get() public view returns (uint) {
       return x;
   }
}

contract C2  {

   C1 c1;

   constructor(address _t) public {
       c1 = C1(_t);
   }

   function get() public view returns (uint result) {
       return c1.get();
   }

   function set(uint _val) public {
       c1.set(_val);
   }

}
```

## Deny transactions sent to a simple contract by non-party node

tags: single, deny

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fail to execute simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"

## Privacy is maintained when non-party node trying to send a transaction to a simple contract

tags: single

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fire and forget execution of simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"
* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"

## Deny transactions sent to a nested contract by non-party node

tags: nested, deny

* Deploy a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute contract `C2`("childContractC2_12")'s `set()` function with new arbitrary value in "Node2" and it's private for "Node1"


## Privacy is maintained when non-party node trying to send a transaction to a nested contract

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

tags: nested

* Deploy a C1 contract with initial value "30" in "Node1"'s default account and it's private for "Node4", named this contract as "parentContractC1_14"
* "parentContractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a C2 contract with initial value "parentContractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "childContractC2_12"
* "childContractC2_12" is deployed "successfully" in "Node1,Node2"
* Fire and forget execution of contract `C2`("childContractC2_12")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node1" returns "30"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node4" returns "30"
* Contract `C1`("parentContractC1_14")'s `get()` function execution in "Node2" returns "0"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node1" returns "30"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node4" returns "0"
* Contract `C2`("childContractC2_12")'s `get()` function execution in "Node2" returns "0"
