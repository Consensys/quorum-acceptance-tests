pragma solidity ^0.5.17;

import "./C2.sol";

contract C1 {

    uint x;

    constructor(uint initVal) public {
        x = initVal;
    }

    function set(uint newValue) public returns (uint) {
        x = newValue;
        return x;
    }

    function get() public view returns (uint) {
        return x;
    }

    function newContractC2(uint newValue) public {
        C2 c = new C2(address(this));
        c.set(newValue);
    }
}
