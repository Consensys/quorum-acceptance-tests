# State manipulation in private smart contract

A simple smart contract is to store a int value and to provide `get()` and `set()` functions.
Non-participated party from original transaction is trying to modify the private state of the participated party.

* Deploy a simple smart contract with initial value "100" in "Node1"'s default account and it's private for "Node7", named this contract as "contract17".

## Private state is maintained
tags: privacy, state

* Execute "contract17"'s `set()` function with new value "0" in "Node3" and it's private for "Node1".
* "contract17"'s `get()` function execution in "Node1" returns "100".