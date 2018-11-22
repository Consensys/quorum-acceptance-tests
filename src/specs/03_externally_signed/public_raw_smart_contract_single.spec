# Public raw smart contract

 Tags: raw

A smart contract, `ClientReceipt`, logs all the deposits that have been performed.
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

## Contract is deployed and mined

* Deploy raw `SimpleStorage` public smart contract from a default account in "Node1", named this contract as "contract13"
* "contract13" is mined