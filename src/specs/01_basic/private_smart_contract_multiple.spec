# Multiple private smart contracts

 Tags: basic

Sending multiple private smart contracts between nodes and verify if all nodes have received.
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

    function get() public view returns (uint retVal) {
        return storedData;
    }
}
```

* Deploy "10" private smart contracts between a default account in "Node1" and a default account in "Node2"
* Deploy "10" private smart contracts between a default account in "Node2" and a default account in "Node3"
* Deploy "10" private smart contracts between a default account in "Node3" and a default account in "Node4"

## Contracts are mined

 Tags: private, load

* "Node1" has received "10" transactions
* "Node2" has received "20" transactions
* "Node3" has received "20" transactions
* "Node4" has received "10" transactions
