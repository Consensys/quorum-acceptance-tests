# QLight client connected to an mps QLight server node

 Tags: networks/typical::qbft-qlight-mps

These tests are tailored for the configuration of networks/typical::qbft-qlight-mps:

    node 1 ql server (PSIs=[Node1, Node6])
    node 2 standard node
    node 3 standard node
    node 4 standard node
    node 5 ql client (server = node 1, psi = Node1)

For this network we would expect Node1 and Node5 vnodes to be in lockstep e.g. private txs sent to Node5 should be
available on Node1 and vice versa.  Private txs sent to Node6 (the other PSI on the server node) should not be available
on Node1 or Node5 vnodes.

* "Node1" is a qlight server
* "Node5" is a qlight client

## Public transactions sent to qlight server and qlight client are available on all nodes

Deploying to Node1:
* Deploy a public accumulator contract in "Node1"'s default account with initial value "1", name this contract as "accPublic1"
* Verify state updates for public accumulator contract are the same on all nodes: contract="accPublic1", sender="Node1", accumulator.inc-tx-id="accPublic1.inc(1)"

Deploying to Node5:
* Deploy a public accumulator contract in "Node5"'s default account with initial value "1", name this contract as "accPublic5"
* Verify state updates for public accumulator contract are the same on all nodes: contract="accPublic5", sender="Node5", accumulator.inc-tx-id="accPublic5.inc(1)"

## Private transactions sent to qlight client are available on the corresponding mps qlight server PSI and vice versa

Send from some other node, privateFor Node5:
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node5", name this contract as "accPrivate5"
* Verify state updates for StandardPrivate accumulator contract are the same on Node1,Node4,Node5 and are not present on Node2,Node3,Node6: contract="accPrivate5", sender="Node4", accumulator.inc-tx-id="accPrivate5.inc(1)"

Send from some other node, privateFor Node1:
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node1", name this contract as "accPrivate1"
* Verify state updates for StandardPrivate accumulator contract are the same on Node1,Node4,Node5 and are not present on Node2,Node3,Node6: contract="accPrivate1", sender="Node4", accumulator.inc-tx-id="accPrivate1.inc(1)"

* Deploy a "PartyProtection" accumulator contract in "Node3"'s default account with initial value "2" and it's private for "Node5", name this contract as "accPrivatePP5"
* Verify state updates and PartyProtection rules for PartyProtection accumulator contract are the same on Node1,Node3,Node5 and are not present on Node2,Node4,Node6: contract="accPrivatePP5", sender="Node3", accumulator.inc-txt-id="accPrivatePP5.inc(5)","accPrivatePP5.inc(6)"

* Deploy a "PartyProtection" accumulator contract in "Node3"'s default account with initial value "2" and it's private for "Node1", name this contract as "accPrivatePP1"
* Verify state updates and PartyProtection rules for PartyProtection accumulator contract are the same on Node1,Node3,Node5 and are not present on Node2,Node4,Node6: contract="accPrivatePP1", sender="Node3", accumulator.inc-txt-id="accPrivatePP1.inc(5)","accPrivatePP1.inc(6)"

## Private transactions sent to the non-corresponding mps qlight server PSI are not available on the qlight client

Send from some other node, privateFor Node6:
* Deploy a "StandardPrivate" accumulator contract in "Node4"'s default account with initial value "1" and it's private for "Node6", name this contract as "accPrivate6"
* Verify state updates for StandardPrivate accumulator contract are the same on Node4,Node6 and are not present on Node1,Node2,Node3,Node5: contract="accPrivate6", sender="Node4", accumulator.inc-tx-id="accPrivate6.inc(1)"
