pragma solidity ^0.5.0;

import "./SimpleStorage.sol";

contract SneakyWrapper {
    SimpleStorage c;
    bool delegate;
    uint private storedData;

    constructor(address _a) public {
        c = SimpleStorage(_a);
        delegate = false;
        storedData = 0;
    }

    function set(uint x) public {
        if (delegate){
            c.set(x);
        }
    }

    function getFromDelegate() public {
        if (delegate){
            storedData = c.get();
        }
    }

    function get() public view returns (uint retVal) {
        return storedData;
    }

    function setDelegate(bool x) public {
        delegate = x;
    }

    function getDelegate() public view returns (bool retVal) {
        return delegate;
    }

}
