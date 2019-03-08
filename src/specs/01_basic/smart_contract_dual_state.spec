# Dual state implementation when peforming function calls from one smart contract to another

 Tags: basic, dual-state, contract-interaction

References:
1. The dual public and private state [explained in details](https://github.com/jpmorganchase/quorum/blob/eab8d793f946f292954ff8e150645661ba599164/NOTES.md)
2. `STATICCALL` opcode update in [Solc 0.5.0 breaking changes](https://solidity.readthedocs.io/en/latest/050-breaking-changes.html)
3. [Pull Request](https://github.com/jpmorganchase/quorum/pull/592) to handle `StaticCall` with dual state implementation

The following smart contracts are used:

__storec.sol__
```
contract storec {
    uint private storedValue;

    constructor (uint pval) public {
        storedValue = pval;
    }

    function setc(uint x) public {
        storedValue = x;
    }

    function getc() public view returns (uint) {
        return storedValue;
    }
}
```

__storeb.sol__
```
interface storec {
    function setc(uint x) external;

    function getc() external view returns (uint);
}

contract storeb {
    uint private storedValue;
    storec anotherStorage;

    constructor (uint initVal, address _addrc) public {
        storedValue = initVal;
        anotherStorage = storec(_addrc);
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
```

__storea.sol__
```
interface storeb {
    function setb(uint x) external;

    function setc(uint x) external;

    function getb() external view returns (uint);

    function getc() external view returns (uint);
}

contract storea {
    uint private storedValue;
    storeb anotherStorage;

    constructor (uint initVal, address _addrb) public {
        storedValue = initVal;
        anotherStorage = storeb(_addrb);
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
```

## Function calls between all public smart contracts: public -> public -> public

 Tags: all-public

* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c1"
* "c1"'s "getc" function execution in "Node1" returns "1"
* Deploy "storeb" smart contract with contract "c1" initial value "1" from a default account in "Node1", named this contract as "b1"
* "b1"'s "getb" function execution in "Node1" returns "1"
* Deploy "storea" smart contract with contract "b1" initial value "1" from a default account in "Node1", named this contract as "a1"
* "a1"'s "geta" function execution in "Node1" returns "1"
* "a1"'s "setc" function execution in "Node1" with value "10"
* "a1"'s "getc" function execution in "Node1" returns "10"
* "a1"'s "setb" function execution in "Node1" with value "10"
* "a1"'s "getb" function execution in "Node1" returns "100"
* "a1"'s "seta" function execution in "Node1" with value "10"
* "a1"'s "geta" function execution in "Node1" returns "1000"
* "b1"'s "getb" function execution in "Node1" returns "100"
* "c1"'s "getc" function execution in "Node1" returns "10"

## Function calls between all private smart contracts: private -> private -> private

 Tags: all-private

* Deploy "storec" smart contract with initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "c2"
* "c2"'s "getc" function execution in "Node1" returns "1"
* Deploy "storeb" smart contract with contract "c2" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "b2"
* "b2"'s "getb" function execution in "Node1" returns "1"
* Deploy "storea" smart contract with contract "b2" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "a2"
* "a2"'s "geta" function execution in "Node1" returns "1"
* "a2"'s "setc" function execution in "Node1" with value "10" and its private for "Node2"
* "a2"'s "getc" function execution in "Node1" returns "10"
* "a2"'s "setb" function execution in "Node1" with value "10" and its private for "Node2"
* "a2"'s "getb" function execution in "Node1" returns "100"
* "a2"'s "seta" function execution in "Node1" with value "10" and its private for "Node2"
* "a2"'s "geta" function execution in "Node1" returns "1000"
* "b2"'s "getb" function execution in "Node1" returns "100"
* "c2"'s "getc" function execution in "Node1" returns "10"

## Function calls from a private smart contract to public smart contracts: private -> public -> public

 Tags: private-to-public

* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c3"
* "c3"'s "getc" function execution in "Node1" returns "1"
* Deploy "storeb" smart contract with contract "c3" initial value "1" from a default account in "Node1", named this contract as "b3"
* "b3"'s "getb" function execution in "Node1" returns "1"
* Deploy "storea" smart contract with contract "b3" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "a3"
* "a3"'s "geta" function execution in "Node1" returns "1"
* "a3"'s "setc" function execution in "Node1" with value "10" and its private for "Node2", should fail
* "a3"'s "getc" function execution in "Node1" returns "1"
* "a3"'s "setb" function execution in "Node1" with value "10" and its private for "Node2", should fail
* "a3"'s "getb" function execution in "Node1" returns "1"
* "a3"'s "seta" function execution in "Node1" with value "10" and its private for "Node2"
* "a3"'s "geta" function execution in "Node1" returns "10"
* "b3"'s "getb" function execution in "Node1" returns "1"
* "c3"'s "getc" function execution in "Node1" returns "1"


## Function calls from a public smart contract to private smart contracts: public -> private -> private

 Tags: public-to-private

* Deploy "storec" smart contract with initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "c4"
* "c4"'s "getc" function execution in "Node1" returns "1"
* Deploy "storeb" smart contract with contract "c4" initial value "1" from a default account in "Node1" and it's private for "Node2", named this contract as "b4"
* "b4"'s "getb" function execution in "Node1" returns "1"
* Deploy "storea" smart contract with contract "b4" initial value "1" from a default account in "Node1", named this contract as "a4"
* "a4"'s "geta" function execution in "Node1" returns "1"
* "a4"'s "setc" function execution in "Node1" with value "10", should fail
* "a4"'s "getc" function execution in "Node1" should fail
* "a4"'s "setb" function execution in "Node1" with value "10", should fail
* "a4"'s "getb" function execution in "Node1" should fail
* "a4"'s "seta" function execution in "Node1" with value "10", should fail
* "a4"'s "geta" function execution in "Node1" returns "1"
* "b4"'s "getb" function execution in "Node1" returns "1"
* "c4"'s "getc" function execution in "Node1" returns "1"
