pragma solidity ^0.5.0;

import "./SimpleStorage.sol";

contract StorageMaster {
    event contractCreated(address _addr);

    SimpleStorage public c;
    SimpleStorage public c2;
    SimpleStorage public c3;

    function createSimpleStorage(uint initVal) public {
        c = new SimpleStorage(initVal);
        emit contractCreated(address(c));
    }

    function createSimpleStorageC2C3(uint initVal) public {
        c2 = new SimpleStorage(initVal);
        emit contractCreated(address(c2));
        c3 = new SimpleStorage(initVal);
        emit contractCreated(address(c3));
    }

    function setC2C3Value(uint val) public{
        c2.set(val);
        c3.set(val);
    }

    function getC2C3Value() public view returns (uint){
        return c2.get() + c3.get();
    }
}
