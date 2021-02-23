# Partial Network upgrade from an old version (q21.1.0/t21.1.0) to an MPS capable version of `geth` and `tessera`

 Tags: mps-upgrade, pre-condition/no-record-blocknumber

        | q_from_version | q_to_version | t_from_version | t_to_version |
        | 21.1.0         | latest       | 21.1.0         | latest       |

* Start the network with:
    | node  | quorum           | tessera          |
    |-------|------------------|------------------|
    | Node1 | <q_from_version> | <t_from_version> |
    | Node2 | <q_from_version> | <t_from_version> |
    | Node3 | <q_from_version> | <t_from_version> |
    | Node4 | <q_from_version> | <t_from_version> |


* Record the current block number, named it as "recordedBlockNumber"

## Run selective node upgrade & downgrade

// Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

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

// private contract
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node1,Node2,Node3", name this contract as "accPrivate1"
* Transaction Hash is returned for "accPrivate1"
* Transaction Receipt is present in "Node1" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node2" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node3" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node4" for "accPrivate1" from "Node4"'s default account
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "1"
* Subscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"
* Invoke a "StandardPrivate" accumulator.inc with value "1" in contract "accPrivate1" in "Node4"'s default account and it's private for "Node1,Node2", file transaction hash as "accPrivate1.inc(1)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node2" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node2" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node3" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node3" for "accPrivate1.inc(1)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "2"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on "Node1" has value "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on "Node2" has value "2"
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
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "4"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node1" has value "4"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "1"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node4" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node4" has value "4"

* Invoke a "StandardPrivate" accumulator.inc with value "3" in contract "accPrivate1" in "Node4"'s default account and it's private for "Node2", file transaction hash as "accPrivate1.inc(3)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(3)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(3)" has no logs
* Transaction Receipt is present in "Node2" for "accPrivate1.inc(3)"
* Transaction Receipt in "Node2" for "accPrivate1.inc(3)" has one IncEvent log with value "5"
* Transaction Receipt is present in "Node3" for "accPrivate1.inc(3)"
* Transaction Receipt in "Node3" for "accPrivate1.inc(3)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(3)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(3)" has one IncEvent log with value "7"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "4"
* Accumulator "accPrivate1"'s `get()` function execution in "Node2" returns "5"
* Accumulator "accPrivate1"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in "Node4" returns "7"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node2" has value "5"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node4" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate1" on "Node4" has value "7"

* Unsubscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"




//***********************************
//***** upgrade quorum in Node1 *****
//***********************************
* Stop and start "Node1" using quorum version <q_to_version> and tessera version <t_from_version>




* Deploy a public accumulator contract in "Node1"'s default account with initial value "1", name this contract as "accPublic2"
* Transaction Hash is returned for "accPublic2"
* Transaction Receipt is present in "Node1" for "accPublic2" from "Node1"'s default account
* Transaction Receipt is present in "Node2" for "accPublic2" from "Node1"'s default account
* Transaction Receipt is present in "Node3" for "accPublic2" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for "accPublic2" from "Node1"'s default account
* Subscribe to accumulator contract "accPublic2" IncEvent on "Node1,Node2,Node3,Node4"
* Accumulator "accPublic2"'s `get()` function execution in "Node1" returns "1"
* Accumulator "accPublic2"'s `get()` function execution in "Node2" returns "1"
* Accumulator "accPublic2"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPublic2"'s `get()` function execution in "Node4" returns "1"
* Invoke accumulator.inc with value "1" in contract "accPublic2" in "Node1"'s default account, file transaction hash as "accPublic2.inc(1)"
* Transaction Receipt is present in "Node1" for "accPublic2.inc(1)"
* Transaction Receipt is present in "Node2" for "accPublic2.inc(1)"
* Transaction Receipt is present in "Node3" for "accPublic2.inc(1)"
* Transaction Receipt is present in "Node4" for "accPublic2.inc(1)"
* Accumulator "accPublic2"'s `get()` function execution in "Node1" returns "2"
* Accumulator "accPublic2"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPublic2"'s `get()` function execution in "Node3" returns "2"
* Accumulator "accPublic2"'s `get()` function execution in "Node4" returns "2"
* Wait for events poll
* Check IncEvent "0" for accumulator contract "accPublic2" on "Node1" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic2" on "Node2" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic2" on "Node3" has value "2"
* Check IncEvent "0" for accumulator contract "accPublic2" on "Node4" has value "2"
* Unsubscribe to accumulator contract "accPublic2" IncEvent on "Node1,Node2,Node3,Node4"

// private contract
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node1,Node2,Node3", name this contract as "accPrivate2"
* Transaction Hash is returned for "accPrivate2"
* Transaction Receipt is present in "Node1" for "accPrivate2" from "Node4"'s default account
* Transaction Receipt is present in "Node2" for "accPrivate2" from "Node4"'s default account
* Transaction Receipt is present in "Node3" for "accPrivate2" from "Node4"'s default account
* Transaction Receipt is present in "Node4" for "accPrivate2" from "Node4"'s default account
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "1"
* Subscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"
* Invoke a "StandardPrivate" accumulator.inc with value "1" in contract "accPrivate2" in "Node4"'s default account and it's private for "Node1,Node2", file transaction hash as "accPrivate2.inc(1)"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node2" for "accPrivate2.inc(1)"
* Transaction Receipt in "Node2" for "accPrivate2.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node3" for "accPrivate2.inc(1)"
* Transaction Receipt in "Node3" for "accPrivate2.inc(1)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate2.inc(1)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(1)" has one IncEvent log with value "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "2"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate2" on "Node1" has value "2"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node2" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate2" on "Node2" has value "2"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node4" is "1"
* Check IncEvent "0" for accumulator contract "accPrivate2" on "Node4" has value "2"

* Invoke a "StandardPrivate" accumulator.inc with value "2" in contract "accPrivate2" in "Node4"'s default account and it's private for "Node1", file transaction hash as "accPrivate2.inc(2)"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(2)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(2)" has one IncEvent log with value "4"
* Transaction Receipt is present in "Node2" for "accPrivate2.inc(2)"
* Transaction Receipt in "Node2" for "accPrivate2.inc(2)" has no logs
* Transaction Receipt is present in "Node3" for "accPrivate2.inc(2)"
* Transaction Receipt in "Node3" for "accPrivate2.inc(2)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate2.inc(2)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "4"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "2"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "4"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate2" on "Node1" has value "4"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node2" is "1"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node4" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate2" on "Node4" has value "4"

* Invoke a "StandardPrivate" accumulator.inc with value "3" in contract "accPrivate2" in "Node4"'s default account and it's private for "Node2", file transaction hash as "accPrivate2.inc(3)"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(3)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(3)" has no logs
* Transaction Receipt is present in "Node2" for "accPrivate2.inc(3)"
* Transaction Receipt in "Node2" for "accPrivate2.inc(3)" has one IncEvent log with value "5"
* Transaction Receipt is present in "Node3" for "accPrivate2.inc(3)"
* Transaction Receipt in "Node3" for "accPrivate2.inc(3)" has no logs
* Transaction Receipt is present in "Node4" for "accPrivate2.inc(3)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(3)" has one IncEvent log with value "7"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "4"
* Accumulator "accPrivate2"'s `get()` function execution in "Node2" returns "5"
* Accumulator "accPrivate2"'s `get()` function execution in "Node3" returns "1"
* Accumulator "accPrivate2"'s `get()` function execution in "Node4" returns "7"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "2"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node2" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate2" on "Node2" has value "5"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node4" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate2" on "Node4" has value "7"

* Unsubscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"

// **********************************************************
// check that the old tx receipts/logs can still be retrieved
// **********************************************************

// check the accPublic1 contract on Node1 and invoke inc(2)
* Subscribe to accumulator contract "accPublic1" IncEvent on "Node1,Node2,Node3,Node4"
* Transaction Receipt is present in "Node1" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node2" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node3" for "accPublic1" from "Node1"'s default account
* Transaction Receipt is present in "Node4" for "accPublic1" from "Node1"'s default account
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

* Invoke accumulator.inc with value "2" in contract "accPublic1" in "Node1"'s default account, file transaction hash as "accPublic1.inc(2)"
* Transaction Receipt is present in "Node1" for "accPublic1.inc(2)"
* Transaction Receipt is present in "Node2" for "accPublic1.inc(2)"
* Transaction Receipt is present in "Node3" for "accPublic1.inc(2)"
* Transaction Receipt is present in "Node4" for "accPublic1.inc(2)"

* Wait for events poll
* Check IncEvent "1" for accumulator contract "accPublic1" on "Node1" has value "4"
* Check IncEvent "1" for accumulator contract "accPublic1" on "Node2" has value "4"
* Check IncEvent "1" for accumulator contract "accPublic1" on "Node3" has value "4"
* Check IncEvent "1" for accumulator contract "accPublic1" on "Node4" has value "4"


* Unsubscribe to accumulator contract "accPublic1" IncEvent on "Node1,Node2,Node3,Node4"


// check the accPrivate1 contract on Node1 and invoke inc(4)
* Subscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "4"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node1" has value "4"

* Invoke a "StandardPrivate" accumulator.inc with value "4" in contract "accPrivate1" in "Node1"'s default account and it's private for "Node4", file transaction hash as "accPrivate1.inc(4)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(4)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(4)" has one IncEvent log with value "8"
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(4)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(4)" has one IncEvent log with value "11"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate1" on "Node1" has value "8"

* Unsubscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"

// TODO - run a trace on
// - accPrivate1.inc(1) - added on Node1 when running 21.1.0
// - accPrivate1.inc(4) - added on Node1 when running latest
// - accPrivate2.inc(3) - added on Node1 when running latest



//*************************************
//***** downgrade quorum in Node1 *****
//*************************************
* Stop and start "Node1" using quorum version <q_from_version> and tessera version <t_from_version>


// check the accPrivate1 contract on Node1 and invoke inc(5)
* Subscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"
* Transaction Receipt is present in "Node1" for "accPrivate1" from "Node4"'s default account
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(2)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate1"'s `get()` function execution in "Node1" returns "8"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate1" on "Node1" has value "8"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node1" has value "4"
* Check IncEvent "0" for accumulator contract "accPrivate1" on "Node1" has value "2"

* Invoke a "StandardPrivate" accumulator.inc with value "5" in contract "accPrivate1" in "Node1"'s default account and it's private for "Node4", file transaction hash as "accPrivate1.inc(5)"
* Transaction Receipt is present in "Node1" for "accPrivate1.inc(5)"
* Transaction Receipt in "Node1" for "accPrivate1.inc(5)" has one IncEvent log with value "13"
* Transaction Receipt is present in "Node4" for "accPrivate1.inc(5)"
* Transaction Receipt in "Node4" for "accPrivate1.inc(5)" has one IncEvent log with value "16"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "4"
* Check IncEvent "3" for accumulator contract "accPrivate1" on "Node1" has value "13"

* Unsubscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"

// check the accPrivate2 contract on Node1 and invoke inc(4)
* Subscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"
* Transaction Receipt is present in "Node1" for "accPrivate2" from "Node4"'s default account
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(1)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(2)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate2"'s `get()` function execution in "Node1" returns "4"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate2" on "Node1" has value "4"

* Invoke a "StandardPrivate" accumulator.inc with value "4" in contract "accPrivate2" in "Node1"'s default account and it's private for "Node4", file transaction hash as "accPrivate2.inc(4)"
* Transaction Receipt is present in "Node1" for "accPrivate2.inc(4)"
* Transaction Receipt in "Node1" for "accPrivate2.inc(4)" has one IncEvent log with value "8"
* Transaction Receipt is present in "Node4" for "accPrivate2.inc(4)"
* Transaction Receipt in "Node4" for "accPrivate2.inc(4)" has one IncEvent log with value "11"
* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate2" on "Node1" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate2" on "Node1" has value "8"

* Unsubscribe to accumulator contract "accPrivate2" IncEvent on "Node1,Node2,Node3,Node4"

// TODO - run a trace on (and compare with the previous trace results)
// - accPrivate1.inc(1) - added on Node1 when running 21.1.0
// - accPrivate1.inc(4) - added on Node1 when running latest
// - accPrivate2.inc(3) - added on Node1 when running latest
