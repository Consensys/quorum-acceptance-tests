# fill transaction with private for

 Tags: basic

## Test fill transaction

* Deploy Simple Storage contract using fillTransaction api with an initial value of "129" called from "Node1" private for "Node2". Name this contract as "C1"

## Transaction payload is secured

 Tags: private, transaction, get-quorum-payload

* If fillTransaction is successful, verify that "C1"'s payload is retrievable from "Node1"
* If fillTransaction is successful, verify that "C1"'s payload is retrievable from "Node2"
* If fillTransaction is successful, verify that "C1"'s payload is not retrievable from "Node3"

## Privacy is enforced between parties

 Tags: private

* If fillTransaction is successful, verify that "C1"'s `get()` function execution in "Node1" returns "129"
* If fillTransaction is successful, verify that "C1"'s `get()` function execution in "Node2" returns "129"
* If fillTransaction is successful, verify that "C1"'s `get()` function execution in "Node3" returns "0"
