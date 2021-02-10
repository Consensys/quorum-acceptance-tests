# Accumulator contract - exercise state/receipt/events divergence when transactions are sent to a subset of the original participants

 Tags: mps-bloom

    | Node1 | Node2 | Node3 | Node4 | Node1Node2  | Node1Node2Node3   | AllNodes                |
    | Node1 | Node2 | Node3 | Node4 | Node1,Node2 | Node1,Node2,Node3 | Node1,Node2,Node3,Node4 |

## Check public events are found before and after bloom period passes

  Tags: public

* Deploy a public accumulator contract in <Node1>'s default account with initial value "1", name this contract as "accPublic1"
* Transaction Hash is returned for "accPublic1"
* Transaction Receipt is present in <Node1> for "accPublic1" from <Node1>'s default account
* Transaction Receipt is present in <Node2> for "accPublic1" from <Node1>'s default account
* Transaction Receipt is present in <Node3> for "accPublic1" from <Node1>'s default account
* Transaction Receipt is present in <Node4> for "accPublic1" from <Node1>'s default account
* Subscribe to accumulator contract "accPublic1" IncEvent on <AllNodes>
* Accumulator "accPublic1"'s `get()` function execution in <Node1> returns "1"
* Accumulator "accPublic1"'s `get()` function execution in <Node2> returns "1"
* Accumulator "accPublic1"'s `get()` function execution in <Node3> returns "1"
* Accumulator "accPublic1"'s `get()` function execution in <Node4> returns "1"
* Invoke accumulator.inc with value "1" in contract "accPublic1" in <Node1>'s default account, file transaction hash as "accPublic1.inc(1)"
* Transaction Receipt is present in <Node1> for "accPublic1.inc(1)"
* Transaction Receipt is present in <Node2> for "accPublic1.inc(1)"
* Transaction Receipt is present in <Node3> for "accPublic1.inc(1)"
* Transaction Receipt is present in <Node4> for "accPublic1.inc(1)"
* Accumulator "accPublic1"'s `get()` function execution in <Node1> returns "2"
* Accumulator "accPublic1"'s `get()` function execution in <Node2> returns "2"
* Accumulator "accPublic1"'s `get()` function execution in <Node3> returns "2"
* Accumulator "accPublic1"'s `get()` function execution in <Node4> returns "2"
* Wait for events poll
* Check IncEvent "0" for accumulator contract "accPublic1" on <Node1> has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on <Node2> has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on <Node3> has value "2"
* Check IncEvent "0" for accumulator contract "accPublic1" on <Node4> has value "2"

* <Node1> has received transactions from "accPublic1" which contain "1" log events in state
* Wait for block height is multiple of "4300" by sending arbitrary public transactions on <Node1>
* <Node1> has received transactions from "accPublic1" which contain "1" log events in state

## Check private events are found before and after bloom period passes

  Tags: private

* Deploy a "StandardPrivate" accumulator contract in <Node4>'s default account with initial value "1" and it's private for <Node1Node2Node3>, name this contract as "accPrivate1"
* Transaction Hash is returned for "accPrivate1"
* Transaction Receipt is present in <Node1> for "accPrivate1" from <Node4>'s default account
* Transaction Receipt is present in <Node2> for "accPrivate1" from <Node4>'s default account
* Transaction Receipt is present in <Node3> for "accPrivate1" from <Node4>'s default account
* Transaction Receipt is present in <Node4> for "accPrivate1" from <Node4>'s default account
* Accumulator "accPrivate1"'s `get()` function execution in <Node1> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node2> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node3> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node4> returns "1"
* Subscribe to accumulator contract "accPrivate1" IncEvent on <AllNodes>
* Invoke a "StandardPrivate" accumulator.inc with value "1" in contract "accPrivate1" in <Node4>'s default account and it's private for <Node1Node2>, file transaction hash as "accPrivate1.inc(1)"
* Transaction Receipt is present in <Node1> for "accPrivate1.inc(1)"
* Transaction Receipt in <Node1> for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in <Node2> for "accPrivate1.inc(1)"
* Transaction Receipt in <Node2> for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Transaction Receipt is present in <Node3> for "accPrivate1.inc(1)"
* Transaction Receipt in <Node3> for "accPrivate1.inc(1)" has no logs
* Transaction Receipt is present in <Node4> for "accPrivate1.inc(1)"
* Transaction Receipt in <Node4> for "accPrivate1.inc(1)" has one IncEvent log with value "2"
* Accumulator "accPrivate1"'s `get()` function execution in <Node1> returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in <Node2> returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in <Node3> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node4> returns "2"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node1> is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on <Node1> has value "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node2> is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on <Node2> has value "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node3> is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node4> is "1"
* Check IncEvent "0" for accumulator contract "accPrivate1" on <Node4> has value "2"

* Invoke a "StandardPrivate" accumulator.inc with value "2" in contract "accPrivate1" in <Node4>'s default account and it's private for <Node1>, file transaction hash as "accPrivate1.inc(2)"
* Transaction Receipt is present in <Node1> for "accPrivate1.inc(2)"
* Transaction Receipt in <Node1> for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Transaction Receipt is present in <Node2> for "accPrivate1.inc(2)"
* Transaction Receipt in <Node2> for "accPrivate1.inc(2)" has no logs
* Transaction Receipt is present in <Node3> for "accPrivate1.inc(2)"
* Transaction Receipt in <Node3> for "accPrivate1.inc(2)" has no logs
* Transaction Receipt is present in <Node4> for "accPrivate1.inc(2)"
* Transaction Receipt in <Node4> for "accPrivate1.inc(2)" has one IncEvent log with value "4"
* Accumulator "accPrivate1"'s `get()` function execution in <Node1> returns "4"
* Accumulator "accPrivate1"'s `get()` function execution in <Node2> returns "2"
* Accumulator "accPrivate1"'s `get()` function execution in <Node3> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node4> returns "4"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node1> is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on <Node1> has value "4"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node2> is "1"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node3> is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node4> is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on <Node4> has value "4"

* Invoke a "StandardPrivate" accumulator.inc with value "3" in contract "accPrivate1" in <Node4>'s default account and it's private for <Node2>, file transaction hash as "accPrivate1.inc(3)"
* Transaction Receipt is present in <Node1> for "accPrivate1.inc(3)"
* Transaction Receipt in <Node1> for "accPrivate1.inc(3)" has no logs
* Transaction Receipt is present in <Node2> for "accPrivate1.inc(3)"
* Transaction Receipt in <Node2> for "accPrivate1.inc(3)" has one IncEvent log with value "5"
* Transaction Receipt is present in <Node3> for "accPrivate1.inc(3)"
* Transaction Receipt in <Node3> for "accPrivate1.inc(3)" has no logs
* Transaction Receipt is present in <Node4> for "accPrivate1.inc(3)"
* Transaction Receipt in <Node4> for "accPrivate1.inc(3)" has one IncEvent log with value "7"
* Accumulator "accPrivate1"'s `get()` function execution in <Node1> returns "4"
* Accumulator "accPrivate1"'s `get()` function execution in <Node2> returns "5"
* Accumulator "accPrivate1"'s `get()` function execution in <Node3> returns "1"
* Accumulator "accPrivate1"'s `get()` function execution in <Node4> returns "7"

* Wait for events poll
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node1> is "2"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node2> is "2"
* Check IncEvent "1" for accumulator contract "accPrivate1" on <Node2> has value "5"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node3> is "0"
* Check IncEvent list size for accumulator contract "accPrivate1" on <Node4> is "3"
* Check IncEvent "2" for accumulator contract "accPrivate1" on <Node4> has value "7"

* <Node1> has received transactions from "accPrivate1" which contain "2" log events in state
* Wait for block height is multiple of "4300" by sending arbitrary public transactions on <Node1>
* <Node1> has received transactions from "accPrivate1" which contain "2" log events in state
