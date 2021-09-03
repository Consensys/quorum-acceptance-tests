// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.5.17;

import "./SimpleStorage.sol";

contract SimpleStorageDelegate {
    SimpleStorage c;
    event SimpleStorageDelegateSet(uint value);

    constructor(address _a) public {
        c = SimpleStorage(_a);
    }

    function set(uint x) public {
        c.set(x);
        emit SimpleStorageDelegateSet(x);
    }

    function get() public view returns (uint retVal) {
        return c.get();
    }
}
