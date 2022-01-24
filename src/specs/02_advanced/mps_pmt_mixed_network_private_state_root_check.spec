# Mixed network with three non MPS capable quorum nodes and one MPS capable/enabled node

This is a clone of the MPS mixed network test. The purpose of this test is to verify that when PMT is enabled the
private state roots match and that the PMT account is not part of the private state.

 Tags: mps-pmt-mixed-network-psr-check, pre-condition/no-record-blocknumber

        | q_non_mps_version | q_mps_version | t_non_mps_version | t_mps_version |
        | develop           | develop       | develop           | develop       |

* Start the network with:
    | node  | quorum              | tessera             |
    |-------|---------------------|---------------------|
    | Node1 | <q_mps_version>     | <t_mps_version>     |
    | Node2 | <q_non_mps_version> | <t_non_mps_version> |
    | Node3 | <q_non_mps_version> | <t_non_mps_version> |
    | Node4 | <q_non_mps_version> | <t_non_mps_version> |


* Record the current block number, named it as "recordedBlockNumber"

## Run mixed network and check matching private state roots

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

// public contract
* Deploy a public accumulator contract in "Node1"'s default account with initial value "1", name this contract as "accPublic1"
* Transaction Hash is returned for "accPublic1"
* Transaction Receipt is present in "Node1" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node2" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node3" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for "accPublic1" from "Node1"'s default account
* Subscribe to accumulator contract "accPublic1" IncEvent on "Node1,Node2,Node3,Node4"
* Accumulator "accPublic1"'s `get()` function execution in "Node1" returns "1"
* Accumulator "accPublic1"'s `get()` function execution in "Node2" returns "1"
* Accumulator "accPublic1"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPublic1"'s `get()` function execution in "Node4" returns "1"
* Invoke accumulator.inc with value "1" in contract "accPublic1" in "Node1"'s default account, file transaction hash as "accPublic1.inc(1)"
* Transaction Receipt is present in "Node1" for "accPublic1.inc(1)"
* Transaction Receipt is present in "Node2" for "accPublic1.inc(1)"
* Transaction Receipt is present in "Node3" for "accPublic1.inc(1)"
* Transaction Receipt is present in "Node4" for "accPublic1.inc(1)"
* Accumulator "accPublic1"'s `get()` function execution in "Node1" returns "2"
* Accumulator "accPublic1"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPublic1"'s `get()` function execution in "Node3" returns "2"
* Accumulator "accPublic1"'s `get()` function execution in "Node4" returns "2"
* Wait for events poll
* Check IncEvent "0" for accumulator contract "accPublic1" on "Node1" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on "Node2" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on "Node3" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on "Node4" has value "2"
* Unsubscribe to accumulator contract "accPublic1" IncEvent on "Node1,Node2,Node3,Node4"

// Deploy a private contract from Node4 to Node1 (first private state on Node1)
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node1", name this contract as "accPrivate1"
* Transaction Hash is returned for "accPrivate1"
* Transaction Receipt is present in "Node1" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node2" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node3" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node4" for "accPrivate1" from "Node4"'s default account
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "1"
* Subscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"
* Invoke a "StandardPrivate" accumulator.inc with value "1" in contract "accPrivate1" in "Node4"'s default account and it's private for "Node1,Node2,Node3", file transaction hash as "accPrivate1.inc(1)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node2" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node2" for "accPrivate1.inc(1)" has no logs
* Transaction Receipt is present in "Node3" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node3" for "accPrivate1.inc(1)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "2"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on "Node1" has value "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node4" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on "Node4" has value "2"

* Invoke a "StandardPrivate" accumulator.inc with value "2" in contract "accPrivate1" in "Node4"'s default account and it's private for "Node1", file transaction hash as "accPrivate1.inc(2)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Transaction Receipt is present in "Node2" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node2" for "accPrivate1.inc(2)" has no logs
* Transaction Receipt is present in "Node3" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node3" for "accPrivate1.inc(2)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "4"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "0"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "4"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node1" has value "4"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node4" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node4" has value "4"

* Unsubscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"


// Deploy a party protection private contract from Node3 to Node2 (second private state on Node1)
* Deploy a "PartyProtection" accumulator contract in "Node3"'s default account with initial value "2" and it's private for "Node2", name this contract as "accPrivate2"
* Transaction Hash is returned for "accPrivate2"
* Transaction Receipt is present in "Node1" for "accPrivate2" from "Node3"'s default account
* Transaction Receipt is present in "Node2" for "accPrivate2" from "Node3"'s default account
* Transaction Receipt is present in "Node3" for "accPrivate2" from "Node3"'s default account
* Transaction Receipt is present in "Node4" for "accPrivate2" from "Node3"'s default account
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "0"
* Subscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"
* Invoke a "PartyProtection" accumulator.inc with value "5" in contract "accPrivate2" in "Node3"'s default account and it's private for "Node2", file transaction hash as "accPrivate2.inc(5)"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(5)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(5)" has no logs
* Transaction Receipt is present in "Node2" for "accPrivate2.inc(5)"
* Transaction Receipt in "Node2" for "accPrivate2.inc(5)" has one IncEvent log with value "7"
* Transaction Receipt is present in "Node3" for "accPrivate2.inc(5)"
* Transaction Receipt in "Node3" for "accPrivate2.inc(5)" has one IncEvent log with value "7"
* Transaction Receipt is present in "Node4" for "accPrivate2.inc(5)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(5)" has no logs
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "0"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "0"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node2" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate2" on "Node2" has value "7"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node3" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate2" on "Node3" has value "7"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node4" is "0"

* Unsubscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"


// trying to invoke as PartyProtection from non party (fails on Node1 private state - as Node1 is not party to accPrivate2)
* Invoking a "PartyProtection" accumulator.inc with value "6" in contract "accPrivate2" in "Node1"'s default account and it's private for "Node2" fails with error "contract not found. cannot transact"
// trying to invoke as PSV (fails on Node2 private state and the privacy flag is different from the accPrivate2 contract privacy flag - PartyProtection)
* Invoking a "StateValidation" accumulator.inc with value "6" in contract "accPrivate2" in "Node2"'s default account and it's private for "Node3" fails with error "sent privacy flag doesn't match all affected contract flags"
// trying to invoke from non party as standard private from Node1 private state
* Invoke a "StandardPrivate" accumulator.inc with value "6" in contract "accPrivate2" in "Node1"'s default account and it's private for "Node2", file transaction hash as "accPrivate2.inc(6)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(6)" has status "0x1"
* Transaction Receipt in "Node2" for "accPrivate2.inc(6)" has status "0x0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "0"
// trying to invoke from non party as StandardPrivate from Node4
* Invoke a "StandardPrivate" accumulator.inc with value "7" in contract "accPrivate2" in "Node4"'s default account and it's private for "Node2", file transaction hash as "accPrivate2.inc(7)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(7)" has status "0x1"
* Transaction Receipt in "Node2" for "accPrivate2.inc(7)" has status "0x0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "0"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "0"


// deploy more contracts on Node4<->Node1 and Node3<->Node2 pairs then check the private state roots
* Deploy a "StandardPrivate" storage master contract in "Node1"'s default account and it's private for "Node4", named this contract as "smPrivate1"

* Transaction Hash is returned for "smPrivate1"
* Transaction Receipt is present in "Node1" for "smPrivate1" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for "smPrivate1" from "Node1"'s default account
* Deploy a "StandardPrivate" simple storage from master storage contract "smPrivate1" in "Node1"'s default account and it's private for "Node4", named this contract as "ssPrivate1"
* "ssPrivate1"'s `get()` function execution in "Node1" returns "10"
* "ssPrivate1"'s `get()` function execution in "Node2" returns "0"
* "ssPrivate1"'s `get()` function execution in "Node3" returns "0"
* "ssPrivate1"'s `get()` function execution in "Node4" returns "10"


* Deploy a "StateValidation" storage master contract in "Node2"'s default account and it's private for "Node3", named this contract as "smPrivate2"

* Transaction Hash is returned for "smPrivate2"
* Transaction Receipt is present in "Node2" for "smPrivate2" from "Node2"'s default account
* Transaction Receipt is present in "Node3" for "smPrivate2" from "Node2"'s default account
* Deploy a "StateValidation" simple storage C2C3 with value "5" from master storage contract "smPrivate2" in "Node2"'s default account and it's private for "Node3"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node1" returns "0"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node2" returns "10"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node3" returns "10"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node4" returns "0"
* Invoke a "StateValidation" setC2C3Value with value "15" in master storage contract "smPrivate2" in "Node2"'s default account and it's private for "Node3"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node1" returns "0"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node2" returns "30"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node3" returns "30"
* "smPrivate2"'s `getC2C3Value()` function execution in "Node4" returns "0"

// check that Node1 private state root matches Node4 and that Node2 matches Node3 (and that Node1 and Node2 have different private state roots)
* Record the current block number, named it as "blockNumberAfterStateChanges"
* Retrieve private state root on "Node1" for "blockNumberAfterStateChanges" and name it "Node1_PSR1"
* Retrieve private state root on "Node2" for "blockNumberAfterStateChanges" and name it "Node2_PSR1"
* Retrieve private state root on "Node3" for "blockNumberAfterStateChanges" and name it "Node3_PSR1"
* Retrieve private state root on "Node4" for "blockNumberAfterStateChanges" and name it "Node4_PSR1"
* Check that private state root "Node1_PSR1" is equal to "Node4_PSR1"
* Check that private state root "Node2_PSR1" is equal to "Node3_PSR1"
* Check that private state root "Node1_PSR1" is different from "Node2_PSR1"

* Check on "Node1" that account "0x000000000000000000000000000000000000007a" does not exist at "blockNumberAfterStateChanges"
* Check on "Node2" that account "0x000000000000000000000000000000000000007a" does not exist at "blockNumberAfterStateChanges"
* Check on "Node3" that account "0x000000000000000000000000000000000000007a" does not exist at "blockNumberAfterStateChanges"
* Check on "Node4" that account "0x000000000000000000000000000000000000007a" does not exist at "blockNumberAfterStateChanges"
