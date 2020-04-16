# fill transaction with private for
x
 Tags: basic

## test fill transaction
* Deploy Simple Storage contract using fillTransaction api with an initial value of "129" called from "Node1" private for "Node2". Name this contract as "C1"

## Transaction payload is secured

 Tags: private, transaction

* "C1"'s payload is retrievable from "Node1"
* "C1"'s payload is retrievable from "Node2"
* "C1"'s payload is not retrievable from "Node3"

## Privacy is enforced between parties

 Tags: private

* "C1"'s `get()` function execution in "Node1" returns "129"
* "C1"'s `get()` function execution in "Node2" returns "129"
* "C1"'s `get()` function execution in "Node3" returns "0"
