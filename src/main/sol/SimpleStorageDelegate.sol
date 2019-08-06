pragma solidity ^0.5.0;

import "./SimpleStorage.sol";

contract SimpleStorageDelegate {
    SimpleStorage c;

    constructor(address _a) public {
        c = SimpleStorage(_a);
    }

    function set(uint x) public {
        c.set(x);
    }

    function get() public view returns (uint retVal) {
        return c.get();
    }
}
