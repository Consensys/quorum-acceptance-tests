# Transactions for different block heights

 Tags: advanced, block-heights

This is to verify the basic functionalities when certain block height is reached.
The purpose is to increase the possibility for the network to accept transactions during edge scenarios like:
proposer rotation.

Using the following smart contracts:
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

    function get() public view returns (uint retVal) {
        return storedData;
    }
}

pragma solidity ^0.5.17;

contract ClientReceipt {
    event Deposit(
        address indexed _from,
        bytes32 indexed _id,
        uint _value
    );

    function deposit(bytes32 _id) public payable {
        emit Deposit(msg.sender, _id, msg.value);
    }
}
```

## Private smart contracts for Istanbul consensus

 Tags: istanbul, private, smart-contract

* Capture the current block height, named it as "snapshot"
* Wait for block height is atleast "10" higher than "snapshot" then deploy a private simple smart contract
* Wait for block height is atleast "20" higher than "snapshot" then deploy a private simple smart contract
* Wait for block height is atleast "50" higher than "snapshot" then deploy a private simple smart contract
* Wait for block height is atleast "70" higher than "snapshot" then deploy a private simple smart contract
* Wait for block height is atleast "100" higher than "snapshot" then deploy a private simple smart contract
* Wait for block height is atleast "150" higher than "snapshot" then deploy a private simple smart contract

## Public smart contracts for Istanbul consensus

 Tags: istanbul, public, smart-contract

* Capture the current block height, named it as "snapshot"
* Wait for block height is atleast "10" higher than "snapshot" then deploy a public `ClientReceipt` smart contract
* Wait for block height is atleast "20" higher than "snapshot" then deploy a public `ClientReceipt` smart contract
* Wait for block height is atleast "50" higher than "snapshot" then deploy a public `ClientReceipt` smart contract
* Wait for block height is atleast "70" higher than "snapshot" then deploy a public `ClientReceipt` smart contract
* Wait for block height is atleast "100" higher than "snapshot" then deploy a public `ClientReceipt` smart contract
* Wait for block height is atleast "150" higher than "snapshot" then deploy a public `ClientReceipt` smart contract

## Private smart contracts for Raft consensus

 Tags: raft

* Deploy "100" private smart contracts between a default account in "Node1" and a default account in "Node4"
* "Node1" has received "100" transactions
* "Node4" has received "100" transactions