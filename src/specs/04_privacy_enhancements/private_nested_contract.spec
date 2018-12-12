# Nested private smart contracts

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

* Deploy a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node7", named this contract as "contractC1_1"
* Deploy a C2 contract with initial value "contractC1_1" in "Node1"'s default account and it's private for "Node7", named this contract as "contractC2_1"
## Contracts are mined

 Tags: private, mining

* Nested Contract Transaction Hash is returned for "contractC1_1"
* Nested Contract Transaction Hash is returned for "contractC2_1"
* Nested Contract Transaction Receipt is present in "Node1" for "contractC1_1"
* Nested Contract Transaction Receipt is present in "Node7" for "contractC1_1"
* Nested Contract Transaction Receipt is present in "Node1" for "contractC2_1"
* Nested Contract Transaction Receipt is present in "Node7" for "contractC2_1"

## Privacy is enforced between parties

 Tags: private

* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node7" returns "42"
* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node3" returns "0"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node1" returns "42"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node7" returns "42"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node3" returns "0"

## When there's an update, privacy is still enforced

 Tags: private

* Nested Contract C2 Execute "contractC2_1"'s `set()` function with new value "5" in "Node1" and it's private for "Node7"
* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node1" returns "5"
* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node7" returns "5"
* Nested Contract C1 "contractC1_1"'s `get()` function execution in "Node3" returns "0"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node1" returns "5"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node7" returns "5"
* Nested Contract C2 "contractC2_1"'s `get()` function execution in "Node3" returns "0"