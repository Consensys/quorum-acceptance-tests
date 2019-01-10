# Gas usage for private contracts on participant and non-participant nodes

 Tags: basic-raft, gas, private

Private contracts with insufficient gas should be handled correctly in all scenarios.
In particular, we need to test the following type of scenario:
- Private txn is published with marginal gas
- Minter is NOT a participant to the transaction and accepts the transaction as it cannot calculate full gas requirement
- Block is minted containing the private txn
- Block is validated by a node that IS party to the transaction and which calculates the gas usage as higher than that supplied
- Block is therefore rejected by the validator
Note that intrinsic gas for the simple contract is between 25,224 and 25,352, required gas is approx 97,800

## Private contract with gas below intrinsic gas should be rejected (and not remain pending).

* Get number of nodes and store as "nodecount"

* Private transaction where minter is a participant and gas value is "25100", name this contract as "contract1"
* Contract "contract1" had exception with message "intrinsic gas too low"
* No transactions are pending on node for "contract1"

* Private transaction where minter is not a participant and gas value is "25100", name this contract as "contract2"
* Contract "contract2" had exception with message "intrinsic gas too low"
* No transactions are pending on node for "contract2"

* Check "nodecount" nodes are still running

## Private contract with gas between intrinsic gas and required gas should be rejected (and not remain pending).

* Get number of nodes and store as "nodecount"

* Private transaction where minter is a participant and gas value is "26000", name this contract as "contract3"
* Contract "contract3" had exception with message "not-enough gas"
* No transactions are pending on node for "contract3"

* Private transaction where minter is not a participant and gas value is "26000", name this contract as "contract4"
* Contract "contract4" had exception with message "not-enough gas"
* No transactions are pending on node for "contract4"

* Check "nodecount" nodes are still running

## Private contract with sufficient gas should be accepted.

* Private transaction where minter is a participant and gas value is "98000", name this contract as "contract5"
* Contract "contract5" creation succeeded
* No transactions are pending on node for "contract5"

* Private transaction where minter is not a participant and gas value is "98000", name this contract as "contract6"
* Contract "contract6" creation succeeded
* No transactions are pending on node for "contract6"
