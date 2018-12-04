# Dual state implementation when peforming function calls from one smart contract to another

 Tags: basic, dual-state, contract-interaction

This is to test dual state approach.

The dual state approach separates public and private state by making the core vm environment context aware.

Although not currently implemented it will need to prohibit value transfers and it must initialise all transactions
from accounts on the public state. This means that sending transactions increments the account nonce on the public state
and contract addresses are derived from the public state when initialised by a transaction. For obvious reasons,
contract created by private contracts are still derived from public state.

This is required in order to have consensus over the public state at all times as non-private participants would still
process the transaction on the public state even though private payload can not be decrypted. This means that participants
of a private group must do the same in order to have public consensus. However the creation of the contract and
interaction still occurs on the private state.

It implements support for the following calling model:

S: sender, (X): private, X: public, ->: direction, [ ]: read only mode

1. S -> A -> B
2. S -> (A) -> (B)
3. S -> (A) -> [ B -> C ]

It does not support

1. (S) -> A
2. (S) -> (A)
3. S -> (A) -> B

Implemented "read only" mode for the EVM. Read only mode is checked during any opcode that could potentially modify the state.
If such an opcode is encountered during "read only", it throws an exception.

The EVM is flagged "read only" when a private contract calls in to public state.

The following smart contracts are used:

__storec.sol__
```
contract storec {
   uint public c;

   constructor (uint pval) public {
       c = pval;
   }

   function setc(uint x) public {
       c = x;
   }

   function getc() public view returns (uint retVal) {
       return c;
   }
}
```

__storeb.sol__
```
contract storec {
    function setc(uint x) public;

    function getc() public view returns (uint);
}

contract storeb {
    uint public b;
    storec c;

    constructor (uint initVal, address _addrc) public {
        b = initVal;
        c = storec(_addrc);
    }

    function getc() public view returns (uint retVal) {
        return c.getc();
    }

    function getb() public view returns (uint retVal) {
        return b;
    }

    function setc(uint x) public {
        return c.setc(x);
    }

    function setb(uint x) public {
        uint mc = c.getc();
        b = x * mc;
    }
}
```

__storea.sol__
```
contract storeb {
    function setb(uint x) public;

    function setc(uint x) public;

    function getb() public view returns (uint);

    function getc() public view returns (uint);
}

contract storea {
    uint public a;
    storeb b;

    constructor (uint initVal, address _addrb) public {
        a = initVal;
        b = storeb(_addrb);
    }

    function geta() public view returns (uint retVal) {
        return a;
    }

    function getb() public view returns (uint retVal) {
        return b.getb();
    }

    function getc() public view returns (uint retVal) {
        return b.getc();
    }

    function seta(uint x) public {
        uint mc = b.getb();
        a = x * mc;
    }

    function setb(uint x) public {
        b.setb(x);
    }

    function setc(uint x) public {
        b.setc(x);
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
* "a4"'s "getc" function execution in "Node1" returns "1"
* "a4"'s "setb" function execution in "Node1" with value "10", should fail
* "a4"'s "getb" function execution in "Node1" returns "1"
* "a4"'s "seta" function execution in "Node1" with value "10", should fail
* "a4"'s "geta" function execution in "Node1" returns "1"
* "b4"'s "getb" function execution in "Node1" returns "1"
* "c4"'s "getc" function execution in "Node1" returns "1"
