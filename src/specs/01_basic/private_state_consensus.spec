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

* Deploy a simple smart contract, enabling private state consensus, with initial value "40" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
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

* Deploy a C1 contract, enabling private state consensus, with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC1_1"
* Deploy a C2 contract with initial value "contractC1_1" in "Node1"'s default account and it's private for "Node4", named this contract as "contractC2_1"
* Nested Contract C2 Fails To Execute "contractC2_1"'s `set()` function with new value "7" in "Node1" and it's private for "Node4"
