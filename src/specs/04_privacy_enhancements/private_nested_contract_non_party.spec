# Nested private smart contracts - non party tries to call set and fails

 Tags: privacy

This is to verify that a private smart contract between 2 parties are not accessible by others.
A simple smart contract is to store a int value and to provide `get()` and `set()` functions.
```
pragma solidity ^0.4.15;

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

* Deploy a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node7", named this contract as "contractC1_2"
* Deploy a C2 contract with initial value "contractC1_2" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_2"
## Contracts are mined

 Tags: private, mining

* Nested Contract Transaction Hash is returned for "contractC1_2"
* Nested Contract Transaction Hash is returned for "contractC2_2"
* Nested Contract Transaction Receipt is present in "Node1" for "contractC1_2"
* Nested Contract Transaction Receipt is present in "Node7" for "contractC1_2"
* Nested Contract Transaction Receipt is present in "Node1" for "contractC2_2"
* Nested Contract Transaction Receipt is present in "Node2" for "contractC2_2"

## Party node (Node1) fails to submit transaction to execute set for C2 (when private for is Node2)

 Tags: private

* Nested Contract C2 Fails To Execute "contractC2_2"'s `set()` function with new value "5" in "Node1" and it's private for "Node2"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node7" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node2" returns "0"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node7" returns "0"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node2" returns "0"

## Non party node (Node2) fails to submit transaction to execute set for C2

 Tags: private

* Nested Contract C2 Fails To Execute "contractC2_2"'s `set()` function with new value "7" in "Node2" and it's private for "Node1"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node7" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node2" returns "0"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node7" returns "0"
* Nested Contract C2 "contractC2_2"'s `get()` function execution in "Node2" returns "0"

## Non party node (Node2) fails to submit transaction to execute set for C1

This verifies that quorum on node 2 will reject the transaction because the preemptive check fails to execute.

* Nested Contract C1 Fails To Execute "contractC1_2"'s `set()` function with new value "35" in "Node2" and it's private for "Node1"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node7" returns "42"
* Nested Contract C1 "contractC1_2"'s `get()` function execution in "Node2" returns "0"
