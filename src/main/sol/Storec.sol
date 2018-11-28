pragma solidity ^0.4.21;

contract storec {
    uint public c;

    constructor (uint pval) public {
        c = pval;
    }

    function setc(uint x) public {
        c = x;
    }

    function getc() public view returns (uint retVal) {
        return c;
    }
}