pragma solidity ^0.5.0;

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
}