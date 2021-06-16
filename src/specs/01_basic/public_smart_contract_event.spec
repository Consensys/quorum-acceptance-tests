# Public smart contract with event

 Tags: basic, public

A smart contract, `ClientReceipt`, logs all the deposits that have been performed.
```
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

## Log events in total are captured when executing the contract

 Tags: log-events

* Deploy `ClientReceipt` smart contract from a default account in "Node1", named this contract as "contract12"
* "contract12" is mined
* Execute "contract12"'s `deposit()` function "10" times with arbitrary id and value from "Node1"
* "Node1" has received "10" transactions which contain "10" log events in total
* "Node2" has received "10" transactions which contain "10" log events in total
* "Node3" has received "10" transactions which contain "10" log events in total
* "Node4" has received "10" transactions which contain "10" log events in total
