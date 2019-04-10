# Support Private State Validation in a private smart contract

 Tags: basic, privacy, privacy-enhancement, private-state-validation, qe81

For simplicity, Private State Validation is abbreviated to PSV through out the specification.

PSV is to make sure all participants only apply transactions which result in the same new state.

At the time or writing, the solution is feasible only when list of participants is shared.
Hence PSV is not enabled by default, it's configurable per contract and
being passed via `sendTransaction` RPC call from client.

Limitations:
 - Byzantine party sender would decide not to send data to one of the participant causing this participant's state
   out of sync with others
 - In a situation when concurrent transactions affecting PSV contract, only the first transaction would be successfully applied.
   Others would have failure in their receipts

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

## Private State is validated in all participants

 Tags: validation

We can't really verify the if participants actually validated their states. Assuming they did, their state should return the same value

* Deploy a "StateValidation" contract `SimpleStorage` with initial value "40" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* "contract14"'s `get()` function execution in "Node1" returns "40"
* "contract14"'s `get()` function execution in "Node4" returns "40"

## Deny transactions that are sent to a non-PSV nested contract executing a function in a PSV parent contract

 Tags: nested

C1 is PSV contract and C2 is not. Transactions to C1 that impacts C2 are not allowed

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "Legacy" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4"

## Deny transactions that are sent to a PSV contract reading from a non-PSV contract

C1 is PSV contract and C2 is not. Transactions to C1 that reads from C2 are not allowed.
As C2 is non-PSV contract, C1 state would be impacted and increase the possibility of future transaction failures.

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "Legacy" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"

## Allow transactions that are sent to a non-PSV contract reading from a PSV contract

C1 is non-PSV contract and C2 is. Transactions to C1 that reads from C2 are allowed

* Deploy a "Legacy" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Contract `C2`("contractC2_14")'s `get()` function execution in "Node1" returns "42"

## Deny transactions after creating a nested contract with a different set of participants

Transactions sent to a nested contract which must be private for same set of original participants. Otherwise they will be denied.
Noted that contract creation is still a success.

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node2"
* Fail to execute contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

## Deny transactions sending to a nested contract with a different set of participants

Transactions must be private for same set of original participants. Otherwise they will be denied

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"

## Deny transactions sending to a PSV contract that affects another PSV contract with different set of participants

Inter-contract message calls are only allowed if all contracts have same set of participants

* Deploy a "StateValidation" contract `C1` with initial value "100" in "Node1"'s default account and it's private for "Node2,Node3", named this contract as "contractC1_123"
* "contractC1_123" is deployed "successfully" in "Node1,Node2,Node3"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_123" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_12"
* "contractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute contract `C2`("contractC2_12")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2"
