# Single Private Smart Contract

This is to verify that a private smart contract between 2 parties are not accessible by others.
A simple smart contract is to store a int value and to provide `get()` and `set()` functions.

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node7".

## Contract is mined
tags: private, mining

* Transaction Hash is returned.
* Transaction Receipt is present in "Node1".
* Transaction Receipt is present in "Node7".

## Storage Root storing private smart contract must be the same
tags: private, storage

* Contracts stored in "Node1" and "Node7" must have the same storage root.
* Contracts stored in "Node1" and "Node3" must not have the same storage root.

## Privacy is enforced between parties
tags: privacy

* Smart contract's `get()` function execution in "Node1" returns "42".
* Smart contract's `get()` function execution in "Node7" returns "42".
* Smart contract's `get()` function execution in "Node3" returns "0".

## When there's an update, privacy is still enforced
tags: privacy

* Execute smart contract's `set()` function with new value "5" in "Node1" and it's private for "Node7".
* Smart contract's `get()` function execution in "Node1" returns "5".
* Smart contract's `get()` function execution in "Node7" returns "5".
* Smart contract's `get()` function execution in "Node3" returns "0".