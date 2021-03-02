# Accumulator contract - exercise state/receipt/events divergence when transactions are sent to a subset of the original participants

 Tags: basic, mps

## Deploy a public accumulator contract, invoke the inc() method and verify the emited events

 Tags: public

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
* Trace transaction "accPublic1.inc(1)" on "Node1" name it "Node1.accPublic1.inc(1).Trace"
* Trace transaction "accPublic1.inc(1)" on "Node2" name it "Node2.accPublic1.inc(1).Trace"
* Check that "Node1.accPublic1.inc(1).Trace" is equal to "Node2.accPublic1.inc(1).Trace"


## Deploy a private accumulator contract, invoke the inc() method on a subset of original parties and verify the emited events

 Tags: private
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

* Trace transaction "accPrivate1.inc(1)" on "Node1" name it "Node1.accPrivate1.inc(1).Trace"
* Trace transaction "accPrivate1.inc(1)" on "Node2" name it "Node2.accPrivate1.inc(1).Trace"
* Trace transaction "accPrivate1.inc(1)" on "Node3" name it "Node3.accPrivate1.inc(1).Trace"
* Trace transaction "accPrivate1.inc(1)" on "Node4" name it "Node4.accPrivate1.inc(1).Trace"
* Check that "Node1.accPrivate1.inc(1).Trace" is equal to "Node2.accPrivate1.inc(1).Trace"
* Check that "Node1.accPrivate1.inc(1).Trace" is equal to "Node4.accPrivate1.inc(1).Trace"
* Check that "Node3.accPrivate1.inc(1).Trace" is empty

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

* Trace transaction "accPrivate1.inc(2)" on "Node1" name it "Node1.accPrivate1.inc(2).Trace"
* Trace transaction "accPrivate1.inc(2)" on "Node2" name it "Node2.accPrivate1.inc(2).Trace"
* Trace transaction "accPrivate1.inc(2)" on "Node3" name it "Node3.accPrivate1.inc(2).Trace"
* Trace transaction "accPrivate1.inc(2)" on "Node4" name it "Node4.accPrivate1.inc(2).Trace"
* Check that "Node1.accPrivate1.inc(2).Trace" is equal to "Node4.accPrivate1.inc(2).Trace"
* Check that "Node2.accPrivate1.inc(2).Trace" is empty
* Check that "Node3.accPrivate1.inc(2).Trace" is empty

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

* Trace transaction "accPrivate1.inc(3)" on "Node1" name it "Node1.accPrivate1.inc(3).Trace"
* Trace transaction "accPrivate1.inc(3)" on "Node2" name it "Node2.accPrivate1.inc(3).Trace"
* Trace transaction "accPrivate1.inc(3)" on "Node3" name it "Node3.accPrivate1.inc(3).Trace"
* Trace transaction "accPrivate1.inc(3)" on "Node4" name it "Node4.accPrivate1.inc(3).Trace"
// one has value 5 the other has value 7 - so it is normal for the traces to be different
* Check that "Node2.accPrivate1.inc(3).Trace" is different from "Node4.accPrivate1.inc(3).Trace"
* Check that "Node1.accPrivate1.inc(3).Trace" is empty
* Check that "Node2.accPrivate1.inc(3).Trace" is not empty
* Check that "Node3.accPrivate1.inc(3).Trace" is empty
* Check that "Node4.accPrivate1.inc(3).Trace" is not empty

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node1" is "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node2" is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on "Node2" has value "5"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node3" is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on "Node4" is "3"
* Check IncEvent "2" for accumulator contract "accPrivate1" on "Node4" has value "7"

* Unsubscribe to accumulator contract "accPrivate1" IncEvent on "Node1,Node2,Node3,Node4"
