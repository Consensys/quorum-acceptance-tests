# Support Private State Validation in a private smart contract

 Tags: privacy, private-state-validation, privacy-enhancements, qe81

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


## Transactions sent to simple contract by non-psv node without flag will result in mismatched receipts

 Tags: single

* Deploy a "StateValidation" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fire and forget execution of simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"
* Transaction Receipt is "unsuccessfully" available in "Node1" for "contract14"
* Transaction Receipt is "successfully" available in "Node3" for "contract14"
* Transaction Receipt is "successfully" available in "Node4" for "contract14"

## Privacy is maintained when non-psv node trying to send a transaction to a simple contract without flag

 Tags: single

Transactions, regardless if it succeeds or not, sent by non-party node must not change the private states of the participants

* Deploy a "StateValidation" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fire and forget execution of simple contract("contract14")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node1"
* "contract14"'s `get()` function execution in "Node1" returns "42"
* "contract14"'s `get()` function execution in "Node4" returns "42"
* "contract14"'s `get()` function execution in "Node3" returns "0"

## Deny transactions that are sent to a non-PSV nested contract executing a function in a PSV parent contract

 Tags: nested

C1 is PSV contract and C2 is not. Transactions to C2 that impacts C1 are not allowed -> nested flag mismatch
1st failure = flag mismatch on C1
2nd failure = privacyMetadata not found for C2

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StandardPrivate" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StandardPrivate" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4" with error "sent privacy flag doesn't match all affected contract flags"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4" with error "PrivacyMetadata not found"

## Deny transactions that are sent to a PSV nested contract executing a function in a non-PSV parent contract

 Tags: nested

C1 is non-PSV contract and C2 is PSV. Transactions to C2 that impacts C1 are not allowed -> nested flag mismatch
1st failure = flag mismtach on C2
2nd failure = privacyMetadata not found for C1

* Deploy a "StandardPrivate" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StandardPrivate" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4" with error "sent privacy flag doesn't match all affected contract flags"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4" with error "PrivacyMetadata not found"

## Deny transactions that are sent to a non-PSV contract reading from a PSV contract and non-PSV contract gets updated

 Tags: nested
C1 is PSV contract and C2 is not. Transactions to C2 that reads from C1 and updates C2 are not allowed -> nested flag mismatch
As C2 is non-PSV contract, C1 state would be impacted and increase the possibility of future transaction failures.
1st failure = flag mismatch on C1
2nd failure = privacyMetadata not found for C2

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StandardPrivate" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StandardPrivate" contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"

## Deny transactions that are sent to a PSV contract reading from a non-PSV contract and PSV contract gets updated

 Tags: nested
C1 is non-PSV contract and C2 is PSV. Transactions to C2 that reads from C1 and updates C2 are not allowed -> nested flag mismatch
As C2 is a PSV contract, reading from a non-PSV contract and updating its state can't be allowed as non-PSV contracts aren't guaranteed to have same state
1st failure = flag mismatch on C2
2nd failure = privacyMetadata not found for C1

* Deploy a "StandardPrivate" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StandardPrivate" contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"

## Allow transactions that are sent to a non-PSV contract reading from a PSV contract

Tags: nested

C1 is PSV contract and C2 is not. Transactions to C2 that reads from C1 are allowed

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StandardPrivate" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Contract `C2`("contractC2_14")'s `get()` function execution in "Node1" returns "42"

## Allow transactions that are sent to a PSV contract reading from a non-PSV contract

Tags: nested

C1 is non-PSV contract and C2 is. Transactions to C2 that reads from C1 are allowed

* Deploy a "StandardPrivate" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Contract `C2`("contractC2_14")'s `get()` function execution in "Node1" returns "42"

## Deny transactions after creating a nested contract with a different set of participants

Transactions sent to a nested contract which must be private for same set of original participants. Otherwise they will be denied.
Noted that contract creation is still a success.
Failure = error response from tessera

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node2"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2" with error "Recipients mismatched for Affected Txn"

## Deny transactions sending to a nested contract with a different set of participants

Transactions must be private for same set of original participants. Otherwise they will be denied
Failure = error response from tessera

* Deploy a "StateValidation" contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2" with error "Recipients mismatched for Affected Txn"

## Deny transactions sending to a PSV contract that affects another PSV contract with different set of participants

Inter-contract message calls are only allowed if all contracts have same set of participants
Failure = error response from tessera

* Deploy a "StateValidation" contract `C1` with initial value "100" in "Node1"'s default account and it's private for "Node2,Node3", named this contract as "contractC1_123"
* "contractC1_123" is deployed "successfully" in "Node1,Node2,Node3"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_123" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_12"
* "contractC2_12" is deployed "successfully" in "Node1,Node2"
* Fail to execute "StateValidation" contract `C2`("contractC2_12")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node2" with error "Recipients mismatched for Affected Txn"

## Allow transactions sending to PSV contract reading a public contract and updating PSV contract

C1 is a public contract and C2 is PSV. Transactions to C2 that reads from C1 are allowed.

* Deploy a public contract `C1` with initial value "42" in "Node1"'s default account, named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node2,Node3,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Execute "StateValidation" contract `C2`("contractC2_14")'s `restoreFromC1()` function in "Node1" and it's private for "Node4"

## Deny transactions sending to PSV contract executing a public contract

C1 is a public contract and C2 is PSV. Transactions to C2 that impacts C1 are not allowed -> evm execution error

* Deploy a public contract `C1` with initial value "42" in "Node1"'s default account, named this contract as "contractC1_14"
* "contractC1_14" is deployed "successfully" in "Node1,Node2,Node3,Node4"
* Deploy a "StateValidation" contract `C2` with initial value "contractC1_14" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_14"
* "contractC2_14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StateValidation" contract `C2`("contractC2_14")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node4" with error "JsonRpcError thrown with code -32000. Message: execution reverted"
