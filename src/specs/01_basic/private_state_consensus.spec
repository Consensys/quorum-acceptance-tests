# Private State Consensus

 Tags: basic, privacy, privacy-enhancement, private-state-consensus

At the time or writing, the solution is feasible only when list of participants is shared.
Hence Private State Consensus is not enabled by default, it's configurable per contract and
being passed via `sendTransaction` RPC call from client.

## Private State is in consensus among participants

Given a simple storage smart contract

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

* TXN1, private state consensus "true", deploys a simple smart contract with initial value "40" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14"'s transaction is applied successfully in "Node1"
* "contract14"'s transaction is applied successfully in "Node4"
* "contract14"'s `get()` function execution in "Node1" returns "40"
* "contract14"'s `get()` function execution in "Node4" returns "40"

## Transaction that triggers interaction between a contract which enables private state consensus and the one which doesn't

Given the following nested contracts

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

Whereas C1 enables private state consensus and C2 does not

* TXN1, private state consensus "true", deploys a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_1"
* TXN2, private state consensus "false", deploys a C2 contract with initial value "contractC1_1" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_1"
* TXN3, private state consensus "true", with nested Contract C2 Fails To Execute "contractC2_1"'s `set()` function with new value "7" in "Node1" and it's private for "Node4"
* TXN4, private state consensus "false", with nested Contract C2 Fails To Execute "contractC2_1"'s `set()` function with new value "7" in "Node1" and it's private for "Node4"
The Quorum1 preemptive checks determine that the new transaction affects both a PSC and a non PSC contract and rejects the transaction

## Transaction that attempts to use a different set of participants for a contract

* TXN1, private state consensus "true", deploys a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_1"
* TXN2, private state consensus "true", where Contract C1 executes "contractC1_1"'s `set()` function with new value "7" in "Node1" and it's private for "Node2" fails validation in tessera
The preemptive execution on Node1 finds that TXN2 is affecting C1 (and identifies the transaction that
created it - TXN1). Quorum1 sends TXN2 to Tessera1 with the affected transaction TXN1. Tessera1 validates that
the recipients for TXN1 and TXN2 match. Tessera1 rejects the transaction.

## Transaction that attempts to use a different set of participants for a contract (nested)

* TXN1, private state consensus "true", deploys a C1 contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_1"
* TXN2, private state consensus "true", deploys a C2 contract with initial value "contractC1_1" in "Node1"'s default account and it's private for "Node2", named this contract as "contractC2_1"
* TXN3, private state consensus "true", where nested Contract C2 Executes "contractC2_1"'s `set()` function with new value "7" in "Node1" and it's private for "Node2"
The preemptive execution of the TXN3 finds that both C2_1 and C1_1 contracts are affected by the transaction
and it pushes both TXN1 and TXN2 as affected transactions whith TXN3. Tessera receives the new transaction and validates
that all transactions TXN1, TXN2 and TXN3 have the same participants list (as all of them have have the PSC=true).
Tessera1 rejects the transction.

## Transaction that affects one PSC contract which reads from a non PSC contract

Not sure we should allow this. The node which publishes the transaction will probably apply the new transaction as
successful while the other nodes would reject it (as the state of the PSC contract would rely on different values
for the non PSC contract).

## Transaction that affects one non PSC contract which reads from a PSC contract

I don't see any reason why not to allow this. Still - with no restrictions on the recipients of the non PSC contract the
results may vary on each node.


