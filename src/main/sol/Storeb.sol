pragma solidity ^0.5.0;

interface storec {
    function setc(uint x) external;

    function getc() external view returns (uint);
}

contract storeb {
    uint private storedValue;
    storec anotherStorage;

    constructor (uint initVal, address _addrc) public {
        storedValue = initVal;
        anotherStorage = storec(_addrc);
    }

    function getc() public view returns (uint) {
        return anotherStorage.getc();
    }

    function getb() public view returns (uint) {
        return storedValue;
    }

    function setc(uint x) public {
        return anotherStorage.setc(x);
    }

    function setb(uint x) public {
        uint mc = anotherStorage.getc();
        storedValue = x * mc;
    }
}