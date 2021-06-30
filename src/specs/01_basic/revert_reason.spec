# Revert Reason is present on Transaction Receipts

 Tags: basic, revert-reason, networks/typical::raft, networks/typical::istanbul, networks/typical-besu::ibft2

## For Public Contracts

* Deploy IncreasingSimpleStorage contract with initial value "100" from "Node1"
* Get value from "Node2" matches "100"
* Set value "200" from "Node1"
* Get value from "Node2" matches "200"
* Set value "20" from "Node1" fails with reason "New value should be higher than stored value"

## For Private Contracts

* Deploy private IncreasingSimpleStorage contract with initial value "100" from "Node1" and private for "Node2"
* Get value from "Node2" matches "100"
* Set value "200" from "Node1" and private for "Node2"
* Get value from "Node2" matches "200"
* Set value "20" from "Node1" and private for "Node2" fails with reason "New value should be higher than stored value"

