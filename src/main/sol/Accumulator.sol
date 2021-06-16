pragma solidity ^0.5.17;

contract Accumulator {
  uint public storedData;

  event IncEvent(uint value);

  constructor(uint initVal) public {
    storedData = initVal;
  }

  function inc(uint x) public {
    storedData = storedData + x;
    emit IncEvent(storedData);
  }

  function get() view public returns (uint retVal) {
    return storedData;
  }
}
