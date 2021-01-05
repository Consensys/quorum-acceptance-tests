pragma solidity ^0.5.0;

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
