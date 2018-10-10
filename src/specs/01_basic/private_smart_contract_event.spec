# Private smart contract with event

tags: basic

Log events in total are only available in participated parties.
A private smart contract, `ClientReceipt`, logs all the deposits that have been performed.
```
pragma solidity ^0.4.0;

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

## Log events in total are **only** captured in participated parties when executing the contract

tags: event, log

* Deploy `ClientReceipt` smart contract from a default account in "Node1" and it's private for "Node7", named this contract as "contract17"
* "contract17" is mined
* Execute "contract17"'s `deposit()` function "10" times with arbitrary id and value from "Node1". And it's private for "Node7"
* "Node1" has received "10" transactions which contain "10" log events in total
* "Node7" has received "10" transactions which contain "10" log events in total
* "Node2" has received "10" transactions which contain "0" log events in total
* "Node3" has received "10" transactions which contain "0" log events in total
* "Node4" has received "10" transactions which contain "0" log events in total
* "Node5" has received "10" transactions which contain "0" log events in total
* "Node6" has received "10" transactions which contain "0" log events in total
