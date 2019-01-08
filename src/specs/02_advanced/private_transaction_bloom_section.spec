# Private smart contract with event in the next bloom bit section

  Tags: advanced

This related to __Private Smart Contract With Event__ specification. But verifying after the next bloom bit section is created.

By default, each bloomb bit section contains 4096 blocks (`params/network_params.go`). Due to this high number of blocks is required to execute,
we explicitly use `raft` consensus.


The following smart contract is used:
```
pragma solidity ^0.5.0;

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

* Note: this spec requires a revisit due to high block number setup

## Log events are **only** captured in participated parties when executing the contract

  Tags: raft, pr570

* Deploy `ClientReceipt` smart contract from a default account in "Node1" and it's private for "Node7", named this contract as "contract17"
* "contract17" is mined
* Execute "contract17"'s `deposit()` function "10" times with arbitrary id and value between original parties
* Wait for block height is multiple of "4096" by sending arbitrary public transactions
* "Node1" has received transactions from "contract17" which contain "10" log events in state
* "Node7" has received transactions from "contract17" which contain "10" log events in state
* "Node2" has received transactions from "contract17" which contain "0" log events in state
