pragma solidity ^0.5.0;

interface Storeb {
    function setb(uint x) external;

    function setc(uint x) external;

    function getb() external view returns (uint);

    function getc() external view returns (uint);
}

contract Storea {
    uint private storedValue;
    Storeb anotherStorage;

    constructor (uint initVal, address _addrb) public {
        storedValue = initVal;
        anotherStorage = Storeb(_addrb);
    }

    function geta() public view returns (uint) {
        return storedValue;
    }

    function getb() public view returns (uint) {
        return anotherStorage.getb();
    }

    function getc() public view returns (uint) {
        return anotherStorage.getc();
    }

    function seta(uint x) public {
        uint mc = anotherStorage.getb();
        storedValue = x * mc;
    }

    function setb(uint x) public {
        anotherStorage.setb(x);
    }

    function setc(uint x) public {
        anotherStorage.setc(x);
    }
}
