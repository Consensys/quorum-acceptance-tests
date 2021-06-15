pragma solidity ^0.5.17;

contract SimpleStringStorage {
    string public storedData;
    event valueSet(string _value);

    constructor(string memory _initVal) public {
        storedData = _initVal;
    }

    function set(string memory _x) public {
        storedData = _x;
        emit valueSet(_x);
    }

    function get() public view returns (string memory) {
        return storedData;
    }
}
