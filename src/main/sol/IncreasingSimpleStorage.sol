pragma solidity ^0.5.0;

contract IncreasingSimpleStorage {
    uint private storedData;
    event SimpleStorageSet(uint value);

    constructor(uint initVal) public {
        storedData = initVal;
    }

    function set(uint x) public {
        require(x > storedData, "New value should be higher than stored value");
        storedData = x;
        emit SimpleStorageSet(x);
    }

    function get() public view returns (uint retVal) {
        return storedData;
    }
}
