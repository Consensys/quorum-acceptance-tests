pragma solidity ^0.5.17;

interface Storec {
    function setc(uint x) external;

    function getc() external view returns (uint);
}

contract Storeb {
    uint private storedValue;
    Storec anotherStorage;

    constructor (uint initVal, address _addrc) public {
        storedValue = initVal;
        anotherStorage = Storec(_addrc);
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
