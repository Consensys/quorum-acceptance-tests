# Revert Reason on Transaction Receipts

 Tags: revert-reason, networks/typical::raft, networks/typical::istanbul

* Deploy IncreasingSimpleStorage contract with initial value "100" from "Node1"
* Set value "200" from "Node1"

## Include revert reason

* Get value from "Node1" matches "200"
* Set value "20" from "Node1" fails with reason "teste"
