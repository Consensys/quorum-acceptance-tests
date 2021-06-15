# Storage Master - exercise parent child relationships including creating two children from parent with a single transaction

 Tags: basic

This is to verify that a SimpleStorage smart contract created via a parent StorageMaster works as expected when used in
both public and private transactions.
```
pragma solidity ^0.5.17;

contract SimpleStorage {
    uint private storedData;

    constructor(uint initVal) public {
        storedData = initVal;
    }

    function set(uint x) public {
        storedData = x;
    }

    function get() public constant returns (uint retVal) {
        return storedData;
    }
}

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
```


## Deploy a public storage master and create a simple storage contract from it

 Tags: public, parent-child

* Deploy a public storage master contract in "Node1"'s default account, named this contract as "smPublic1"

* Transaction Hash is returned for "smPublic1"
* Transaction Receipt is present in "Node1" for "smPublic1" from "Node1"'s default account
* Deploy a simple storage from master storage contract "smPublic1" in "Node1"'s default account, named this contract as "ssPublic1"
* "ssPublic1"'s `get()` function execution in "Node1" returns "10"
* "ssPublic1"'s `get()` function execution in "Node2" returns "10"
* "ssPublic1"'s `get()` function execution in "Node4" returns "10"

## Deploy a private storage master and create a simple storage contract from it

 Tags: private, parent-child

* Deploy a "StandardPrivate" storage master contract in "Node1"'s default account and it's private for "Node4", named this contract as "smPrivate1"

* Transaction Hash is returned for "smPrivate1"
* Transaction Receipt is present in "Node1" for "smPrivate1" from "Node1"'s default account
* Deploy a "StandardPrivate" simple storage from master storage contract "smPrivate1" in "Node1"'s default account and it's private for "Node4", named this contract as "ssPrivate1"
* "ssPrivate1"'s `get()` function execution in "Node1" returns "10"
* "ssPrivate1"'s `get()` function execution in "Node2" returns "0"
* "ssPrivate1"'s `get()` function execution in "Node4" returns "10"

## Deploy a private storage master and create two simple storage contracts from it with a single transaction

  Tags: private, parent-child

* Deploy a "StandardPrivate" storage master contract in "Node1"'s default account and it's private for "Node4", named this contract as "smPrivate2"

* Transaction Hash is returned for "smPrivate2"
* Transaction Receipt is present in "Node1" for "smPrivate2" from "Node1"'s default account
* Deploy a "StandardPrivate" simple storage C2C3 with value "5" from master storage contract "smPrivate2" in "Node1"'s default account and it's private for "Node4"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node1" returns "10"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node2" returns "0"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node4" returns "10"
* Invoke a "StandardPrivate" setC2C3Value with value "15" in master storage contract "smPrivate2" in "Node1"'s default account and it's private for "Node4"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node1" returns "30"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node2" returns "0"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node4" returns "30"
