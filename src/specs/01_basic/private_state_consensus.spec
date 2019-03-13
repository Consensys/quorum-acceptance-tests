# Private State Consensus

 Tags: basic, privacy, privacy-enhancement, private-state-consensus

At the time or writing, the solution is feasible only when list of participants is shared.
Hence Private State Consensus is not enabled by default, it's configurable per contract and
being passed via `sendTransaction` RPC call from client.

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

For simplicity, Private State Consensus is abbreviated to PSC through out the specification.

## Private State is in consensus among participants

* Deploy a "PSC" contract `SimpleStorage` with initial value "40" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* "contract14"'s `get()` function execution in "Node1" returns "40"
* "contract14"'s `get()` function execution in "Node4" returns "40"

## Deny transactions that are sent to a non-PSC nested contract executing a function in a PSC parent contract

C1 is PSC contract and C2 is not. Transactions to C1 that impacts C2 are not allowed

* Deploy a "PSC" contract C1 with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "non-PSC" contract C2 with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "contractC2_14"'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4"

## Deny transactions that are sent to a PSC contract reading from a non-PSC contract

C1 is PSC contract and C2 is not. Transactions to C1 that reads from C2 are not allowed

* Deploy a "PSC" contract C1 with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "non-PSC" contract C2 with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "contractC2_14"'s `get()` function in "Node1"

## Allow transactions that are sent to a non-PSC contract reading from a PSC contract

C1 is non-PSC contract and C2 is. Transactions to C1 that reads from C2 are allowed

* Deploy a "non-PSC" contract C1 with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PSC" contract C2 with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* "contractC2_14"'s `get()` function execution in "Node1" returns "42"

## Deny transactions after creating a nested contract with a different set of participants

Transactions sent to a nested contract which must be private for same set of original participants. Otherwise they will be denied.
Noted that contract creation is still a success.

* Deploy a "PSC" contract C1 with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PSC" contract C2 with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node2"
* Fail to execute "contractC2_14"'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

## Deny transactions sending to a nested contract with a different set of participants

Transactions must be private for same set of original participants. Otherwise they will be denied

* Deploy a "PSC" contract C1 with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "PSC" contract C2 with initial value "contractC1_1" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "contractC2_14"'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

## Deny transactions sending to a PSC contract that affects another PSC contract with different set of participants

Inter-contract message calls are only allowed if all contracts have same set of participants

* Deploy a "PSC" contract C1 with initial value "100" in "Node1"'s default account and it's private for "Node2,Node3", named this contract as "contractC1_123"
* "contractC1_123" is deployed "successfully" in "Node1,Node2,Node3"
* Deploy a "PSC" contract C2 with initial value "contractC1_123" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_12"
* "contractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute "contractC2_12"'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
