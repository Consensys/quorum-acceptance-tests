# Extend nested private contracts to new party

 Tags: basic, extension

This is to verify the contract extension APIs (failure scenarios).

There are 2 set of contracts to be used selectively in the below scenarios.

Single smart contract
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

    function get() public constant returns (uint retVal) {
        return storedData;
    }
}
```

Nested smart contracts
```
pragma solidity ^0.5.17;

import "./C2.sol";

contract C1 {

    uint x;

    constructor(uint initVal) public {
        x = initVal;
    }

    function set(uint newValue) public returns (uint) {
        x = newValue;
        return x;
    }

    function get() public view returns (uint) {
        return x;
    }

    function newContractC2(uint newValue) public {
        C2 c = new C2(address(this));
        // method calling C1 set function
        c.set(newValue);
    }
}

pragma solidity ^0.5.17;

import "./C1.sol";

contract C2 {

    C1 c1;
    uint y;

    constructor(address _t) public {
        c1 = C1(_t);
    }

    function get() public view returns (uint result) {
        return c1.get();
    }

    function set(uint _val) public {
        y = _val;
        c1.set(_val);
    }

    function restoreFromC1() public {
        y = c1.get();
    }
}

```
## Re-extend an already extended contract for StandardPrivate - state change should not happen
* Deploy "StandardPrivate" type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract5Extension". Ensure that its not visible from "Node1"

* Initiate "contract5Extension" extension from "Node2" to "Node1". Contract extension accepted in receiving node. Check that state value in receiving node is "42"

* Execute "contract5Extension"'s `set()` function with privacy type "StandardPrivate" to set new value to "999" in "Node2" and it's private for "Node4"
* "contract5Extension"'s `get()` function execution in "Node2" returns "999"
* "contract5Extension"'s `get()` function execution in "Node4" returns "999"
* "contract5Extension"'s `get()` function execution in "Node1" returns "42"


## Allow any party to extend contract for StandardPrivate contract
* Deploy "StandardPrivate" type `SimpleStorage` contract with initial value "42" between "Node2" and "Node4". Name this contract as "contract1Extension". Ensure that its not visible from "Node1"
* Initiate "contract1Extension" extension from "Node4" to "Node1". Contract extension accepted in receiving node. Check that state value in receiving node is "42"
