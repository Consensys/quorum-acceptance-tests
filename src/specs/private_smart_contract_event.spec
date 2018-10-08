# Private smart contract with event

Log events are only available in participated parties.
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

## Log events are captured in participated parties when executing the contract

* Deploy `ClientReceipt` smart contract from a default account in "Node1" and it's private for "Node7", named this contract as "contract17".
* "contract17" is mined.
* Execute "contract17"'s `deposit()` function "10" times with arbitrary id and value from "Node1". And it's private for "Node7".
* "Node1" has received "10" transactions which totally contain "10" log events.
* "Node7" has received "10" transactions which totally contain "10" log events.
* "Node3" has received "10" transactions which totally contain "0" log events.