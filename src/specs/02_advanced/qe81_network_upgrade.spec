# Partial Network upgrade from an old version (q2.5.0/t0.10.5) to a privacy enhancements enabled version of `geth` and `tessera`

 Abbreviations:
 PE - privacy enhancements
 SP - StandatrdPrivate transaction/contract
 PP - PartyProtection transaction/contract
 PSV - PrivateStateValidation transaction/contract

 Upgrade means stoping a docker container that uses image A then rebuild and start it using image B (use the new binaries while keeping the original storage).
 The client (the acceptance tests framework) is PE enabled (is able to specify privacyFlag in transaction parameters).
 The non PE nodes are not aware of the privacyFlag and ignore it (for the old nodes all transactions are SP regardless of the privacyFlag).

 Test description:

 We start a 4 node network (with pre PE versions).
 We upgrade Node1 to a quorum version that supports PE and check that:
 - SP/PSV transactions are rejected by the node (as PE are disabled)
 - we try to enable PE (geth init) with a block height lower than the current block height - expecting error from geth init
 - we try to enable PE (geth init) with a block height higher than the current block height - expecting successs and a warning message that informs that the privacy manager must support PE
 - we try to start geth after enabling PE with the original version of tessera that does not support PE - expecting geth to exit and print error message
 We upgrade Node1 to have both quorum and tessera versions that support PE and check that:
 - quorum starts successfully
 - check that SP transactions work as expected
 - check that PP transactions/contracts work as expected on Node1 (attempted interractions from Node3/Node4 are rejected by Node1 - as incoming transactions are received as SP)
 We upgrade Node4 to have both quorum and tessera versions that support PE (but PE are not enabled yet) and check that:
 - quorum starts successfully
 - when Node4 receives a PP transaction from Node1 it logs a warning that PE metadata is ignored (as PE are disabled)
 We enable PE on Node4 and check that:
 - we deploy a PP contract from Node1 to Node2 and Node4
 - we prove that a non party node (Node3) can't affect the state of the deployed PP contract on Node1 and Node4
 - we prove that PE party nodes can update the state of the PP contract
 - we prove that non PE prarty nodes can't update the state of the PP contract
 - run the above scenario with a SP contract to highlight the different behavior

 Tags: privacy-enhancements-upgrade, pre-condition/no-record-blocknumber

        | q_from_version | q_to_version | t_from_version | t_to_version |
        | v2.5.0         | pe           | 0.10.5         | pe           |

* Start the network with:
    | node  | quorum           | tessera          |
    |-------|------------------|------------------|
    | Node1 | <q_from_version> | <t_from_version> |
    | Node2 | <q_from_version> | <t_from_version> |
    | Node3 | <q_from_version> | <t_from_version> |
    | Node4 | <q_from_version> | <t_from_version> |


* Record the current block number, named it as "recordedBlockNumber"

## Run selective node upgrade

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
 Upgrade quorum in Node1 to a version that supports privacy enhancements while privacy enhancements are disabled
* Stop and start "Node1" using quorum version <q_to_version> and tessera version <t_from_version>
 StandardPrivate transactions work as epected
* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract15"
* "contract15" is deployed "successfully" in "Node1,Node4"
* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node4"'s default account and it's private for "Node1", named this contract as "contract16"
* "contract16" is deployed "successfully" in "Node1,Node4"
 PartyProtection transactions are rejected in quorum Node1 as privacy enhancemetns are not enabled yet
* Deploying a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4" fails with message "PrivacyEnhancements are disabled"
 Run geth init with privacyEnhancementsBlock=currentBlockHeight-1
* Record the current block number, named it as "recordedBlockNumber"
* Stop "quorum" in "Node1"
* Run gethInit in "Node1" with genesis file having privacyEnhancementsBlock set to "recordedBlockNumber" + "-1"
 Geth init fails
* Grep "quorum" in "Node1" for "mismatching Privacy Enhancements fork block in database"
* Check that "quorum" in "Node1" is "down"
* Run gethInit in "Node1" with genesis file having privacyEnhancementsBlock set to "recordedBlockNumber" + "1"
 Geth init succeeds but displays the warning message that quorum won't start unless the privacy manager supports privacy enhancements
* Grep "quorum" in "Node1" for "Please ensure your privacy manager is upgraded and supports privacy enhancements"
 Tessera hasn't been updated so quorum should fail to start
* Grep "quorum" in "Node1" for "Cannot start quorum with privacy enhancements enabled while the transaction manager does not support it"
* Check that "quorum" in "Node1" is "down"
 Upgrade quorum and tessera in Node1 to the privacy enhancements enabled versions (only tessera needs to be updated but it is easier to use the step that does both)
* Stop and start "Node1" using quorum version <q_to_version> and tessera version <t_to_version>
* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract16"
* "contract16" is deployed "successfully" in "Node1,Node4"
 A PartyProtection contract will be deployed on Node1 while on Node4 it will be considered StandardPrivate (Node4 is not upgraded yet)
* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* "contract17" is deployed "successfully" in "Node1,Node4"
 A PartyProtection transaction from Node3 will affect Node4's state but not Node1s's - as it will be received on Node1 as a StandardPrivate transaction
* Fire and forget execution of "PartyProtection" simple contract("contract17")'s `set()` function with new value "5" in "Node3" and it's private for "Node1,Node4"
* "contract17"'s `get()` function execution in "Node1" returns "42"
* "contract17"'s `get()` function execution in "Node4" returns "5"
* "contract17"'s `get()` function execution in "Node3" returns "0"
 A PartyProtection transaction from Node4 will node affect Node1's state as it will be received on Node1 as a StandardPrivate transaction
* Fire and forget execution of "PartyProtection" simple contract("contract17")'s `set()` function with new value "7" in "Node4" and it's private for "Node1"
* "contract17"'s `get()` function execution in "Node1" returns "42"
* "contract17"'s `get()` function execution in "Node4" returns "7"
* Stop and start "Node4" using quorum version <q_to_version> and tessera version <t_to_version>
 Node4 is now privacy enhancements capable but privacy enhancements are not enabled (geth init hasnt been run with a genesis file containing privacyEnhancementsBlock)
* Deploying a "PartyProtection" simple smart contract with initial value "42" in "Node4"'s default account and it's private for "Node1" fails with message "PrivacyEnhancements are disabled"
* Deploy a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract18"
 A warning should be displayed in Node4 logs stating that privacy metadata has been ignored
* Grep "quorum" in "Node4" for "Non StandardPrivate transaction received but PrivacyEnhancements are disabled. Enhanced privacy metadata will be ignored."
 Run geth init to start geth with privacy enhancements enabled
* Record the current block number, named it as "recordedBlockNumber"
* Stop "quorum" in "Node4"
* Run gethInit in "Node4" with genesis file having privacyEnhancementsBlock set to "recordedBlockNumber" + "1"
 Check that geth init issues the warning about the privacy manager supporting privacy enhancements
* Grep "quorum" in "Node4" for "Please ensure your privacy manager is upgraded and supports privacy enhancements"
 Geth init is automatically followed by geth start so geth should already be started
* Check that "quorum" in "Node4" is "up"
 At this stage Node1 and Node4 are fully upgraded and have enabled privacy enhancements while Node2 and Node3 have the original versions
* Deploy a "PartyProtection" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node4", named this contract as "contract19"
* "contract19"'s `get()` function execution in "Node1" returns "42"
* "contract19"'s `get()` function execution in "Node2" returns "42"
* "contract19"'s `get()` function execution in "Node3" returns "0"
* "contract19"'s `get()` function execution in "Node4" returns "42"
 Node3 which is not party to the contract can't update the state of the PE nodes which are party to the contract (Node1 and Node4)
* Fire and forget execution of "PartyProtection" simple contract("contract19")'s `set()` function with new value "5" in "Node3" and it's private for "Node1,Node2,Node4"
* "contract19"'s `get()` function execution in "Node1" returns "42"
 For Node2 all the PP transactions are SP (normal private transactions) - thus the state is updated (even from non party node)
* "contract19"'s `get()` function execution in "Node2" returns "5"
* "contract19"'s `get()` function execution in "Node3" returns "0"
* "contract19"'s `get()` function execution in "Node4" returns "42"
 PP transaction from Node4 updates all party nodes state
* Fire and forget execution of "PartyProtection" simple contract("contract19")'s `set()` function with new value "55" in "Node4" and it's private for "Node1,Node2"
* "contract19"'s `get()` function execution in "Node1" returns "55"
* "contract19"'s `get()` function execution in "Node2" returns "55"
* "contract19"'s `get()` function execution in "Node3" returns "0"
* "contract19"'s `get()` function execution in "Node4" returns "55"
 Even though Node2 is party to the PP contract - it can't update the state on Node1 and Node4 as transactions coming from Node2 are SP for Node1 and Node4. It does however update it's own state.
* Fire and forget execution of "PartyProtection" simple contract("contract19")'s `set()` function with new value "10" in "Node2" and it's private for "Node1,Node4"
* "contract19"'s `get()` function execution in "Node1" returns "55"
* "contract19"'s `get()` function execution in "Node2" returns "10"
* "contract19"'s `get()` function execution in "Node3" returns "0"
* "contract19"'s `get()` function execution in "Node4" returns "55"
 Standard private transactions work as expected (in a mix of old/new nodes)
 Deploy a StandardPrivate simple contract from Node1 to Node2 and Node4
* Deploy a "StandardPrivate" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node4", named this contract as "contract20"
* "contract20"'s `get()` function execution in "Node1" returns "42"
* "contract20"'s `get()` function execution in "Node3" returns "0"
* "contract20"'s `get()` function execution in "Node2" returns "42"
* "contract20"'s `get()` function execution in "Node4" returns "42"
 Update the value in Node4 for Node1 and Node2
* Fire and forget execution of "StandardPrivate" simple contract("contract20")'s `set()` function with new value "7" in "Node4" and it's private for "Node1,Node2"
* "contract20"'s `get()` function execution in "Node1" returns "7"
* "contract20"'s `get()` function execution in "Node2" returns "7"
* "contract20"'s `get()` function execution in "Node3" returns "0"
* "contract20"'s `get()` function execution in "Node4" returns "7"
 Update the value in Node2 for Node1 and Node4
* Fire and forget execution of "StandardPrivate" simple contract("contract20")'s `set()` function with new value "8" in "Node2" and it's private for "Node1,Node4"
* "contract20"'s `get()` function execution in "Node1" returns "8"
* "contract20"'s `get()` function execution in "Node2" returns "8"
* "contract20"'s `get()` function execution in "Node3" returns "0"
* "contract20"'s `get()` function execution in "Node4" returns "8"
 A non party (Node3) can change the state of party nodes
* Fire and forget execution of "StandardPrivate" simple contract("contract20")'s `set()` function with new value "9" in "Node3" and it's private for "Node1,Node2,Node4"
* "contract20"'s `get()` function execution in "Node1" returns "9"
* "contract20"'s `get()` function execution in "Node2" returns "9"
* "contract20"'s `get()` function execution in "Node3" returns "0"
* "contract20"'s `get()` function execution in "Node4" returns "9"
// TODO - Check with Nam if we could add some resend steps here (or leave it for a separate upgrade & resend scenario)
