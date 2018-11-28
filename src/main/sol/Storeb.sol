pragma solidity ^0.4.21;

contract storec {
    function setc(uint x) public;

    function getc() public view returns (uint);
}

contract storeb {
    uint public b;
    storec c;

    constructor (uint initVal, address _addrc) public {
        b = initVal;
        c = storec(_addrc);
    }

    function getc() public view returns (uint retVal) {
        return c.getc();
    }

    function getb() public view returns (uint retVal) {
        return b;
    }

    function setc(uint x) public {
        return c.setc(x);
    }

    function setb(uint x) public {
        uint mc = c.getc();
        b = x * mc;
    }
}