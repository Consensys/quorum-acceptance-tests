// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.5.16;

contract Storec {
    uint private storedValue;

    constructor (uint pval) public {
        storedValue = pval;
    }

    function setc(uint x) public {
        storedValue = x;
    }

    function getc() public view returns (uint) {
        return storedValue;
    }
}
