pragma solidity ^0.5.17;

// this contract is to demonstrate ability to read other contract code/hash/size
// via EXTCODECOPY, EXTCODEHASH and EXTCODESIZE opcodes
contract ContractCodeReader {
    bytes32 private lastCodeHash = 0;
    bytes private lastCode = "";
    uint32 private lastCodeSize = 0;

    function getCodeHash(address _a) view public returns (bytes32) {
        bytes32 ret;
        assembly {
            ret := extcodehash(_a)
        }
        return ret;
    }

    function getCodeSize(address _a) view public returns (uint32) {
        uint32 ret;
        assembly {
            ret := extcodesize(_a)
        }
        return ret;
    }

    function getCode(address _a) view public returns (bytes memory o_code) {
        assembly {
            // retrieve the size of the code, this needs assembly
            let size := extcodesize(_a)
            // allocate output byte array - this could also be done without assembly
            // by using o_code = new bytes(size)
            o_code := mload(0x40)
            // new "memory end" including padding
            mstore(0x40, add(o_code, and(add(add(size, 0x20), 0x1f), not(0x1f))))
            // store length in memory
            mstore(o_code, size)
            // actually retrieve the code, this needs assembly
            extcodecopy(_a, add(o_code, 0x20), 0, size)
        }
    }

    function setLastCodeHash(address _a) public {
        lastCodeHash = getCodeHash(_a);
    }

    function getLasCodeHash() view public returns (bytes32) {
        return lastCodeHash;
    }

    function setLastCode(address _a) public {
        lastCode = getCode(_a);
    }

    function getLastCode() view public returns (bytes memory) {
        return lastCode;
    }

    function setLastCodeSize(address _a) public {
        lastCodeSize = getCodeSize(_a);
    }

    function getLastCodeSize() view public returns (uint32) {
        return lastCodeSize;
    }
}