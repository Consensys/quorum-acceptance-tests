// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.5.16;

contract SimpleStorage {
    uint private storedData;
    event SimpleStorageSet(uint value);

    constructor(uint initVal) public {
        storedData = initVal;
    }

    function set(uint x) public {
        storedData = x;
        emit SimpleStorageSet(x);
    }

    function get() public view returns (uint retVal) {
        return storedData;
    }
}
