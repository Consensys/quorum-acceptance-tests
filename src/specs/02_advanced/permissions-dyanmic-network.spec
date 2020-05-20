# Quorum Permissions model testing with dynamic network

  Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, permissions

## Permissions basic testing
    Tags: post-condition/datadir-cleanup, post-condition/network-cleanup


* Start a "permissioned" Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "full" `gcmode`
* Deploy permissions model contracts from "Node1"
* Create permissions config file and write the file to "Node1,Node2,Node3"
* Restart network "default"
* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Validate that org "NWADMIN" is approved, has "Node2" linked and has role "NWADMIN"
* Validate that org "NWADMIN" is approved, has "Node3" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active

## Permission with new org propose and approval
    Tags: post-condition/datadir-cleanup, post-condition/network-cleanup


* Start a "permissioned" Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "full" `gcmode`
* Deploy permissions model contracts from "Node1"
* Create permissions config file and write the file to "Node1,Node2,Node3"
* Restart network "default"
* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" in "networkId"
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network "default"
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"

## Permissioned network suspend org, ensure no transaction processing for org and then revoke suspension
    Tags: post-condition/datadir-cleanup, post-condition/network-cleanup


* Start a "permissioned" Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "full" `gcmode`
* Deploy permissions model contracts from "Node1"
* Create permissions config file and write the file to "Node1,Node2,Node3"
* Restart network "default"
* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" in "networkId"
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network "default"
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"
* From "Node1" suspend org "ORG1", confirm that org status is "PendingSuspension"
* From "Node1" approve org "ORG1"'s suspension, confirm that org status is "Suspended"
* Deploy "storec" smart contract with initial value "5" from a default account in "Node4" fails with error "read only account. cannot transact"
* From "Node1" revoke suspension of org "ORG1", confirm that org status is "RevokeSuspension"
* From "Node1" approve org "ORG1"'s suspension revoke, confirm that org status is "Approved"
* Deploy "storec" smart contract with initial value "5" from a default account in "Node4", named this contract as "c1"
* "c1"'s "getc" function execution in "Node4" returns "5"


## Permissioned network deactivate a node and ensure that blocks are not syncing
    Tags: post-condition/datadir-cleanup, post-condition/network-cleanup


* Start a "permissioned" Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "full" `gcmode`
* Deploy permissions model contracts from "Node1"
* Create permissions config file and write the file to "Node1,Node2,Node3"
* Restart network "default"
* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" in "networkId"
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network "default"
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"
* From "Node1" deactivate org "NWADMIN"'s node "Node3"
* Check org "NWADMIN" has "Node3" with status "Deactivated"
* Save current blocknumber from "Node3"
* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c2"
* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c3"
* Ensure current blocknumber from "Node3" has not changed

