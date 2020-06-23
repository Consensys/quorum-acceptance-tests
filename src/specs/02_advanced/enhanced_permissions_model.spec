# Enhanced Permissions Model

 Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, permissions

The Enhanced Permissions Model caters for enterprise-level needs by having a smart contract-based permissioning model.
This allows for significant flexibility to manage nodes, accounts, and account-level access controls.

For more information, visit https://docs.goquorum.com/en/latest/Permissioning/Enhanced%20Permissions%20Model/Overview/

The test scenarios are based on the following setup:

      |id        |networkType      |version |
      |r1        |permissioned     |basic   |
      |r2        |permissioned     |eea     |
      
* Start a <networkType> <version> Quorum Network, named it <id>, consisting of "Node1,Node2,Node3"
* Deploy permissions model contracts from "Node1" with <version>
* Create permissions config file and write the file to "Node1,Node2,Node3"
* Restart network <id>

## Network Admin Organization and Role are initialized successfully

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

Network Admin Organization is the name of initial organization that will be created as a part of network boot up with new permissions model.
This organization will own all the initial nodes which come at the time of network boot up and accounts
which will be the network admin account.

Network Admin Role will have full access and will be network admin.
This role will be assigned to the network admin accounts

* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Validate that org "NWADMIN" is approved, has "Node2" linked and has role "NWADMIN"
* Validate that org "NWADMIN" is approved, has "Node3" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active

## Proposing a new organization into the network

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

Once the network is up, the network admin accounts can then propose a new organization into the network.
Majority approval from the network admin accounts is required before an organization is approved.

* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" with <version>
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network <id>
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"

## Suspending an organization temporarily

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

The network admin accounts can suspend/revoke-suspension all activities (e.g.: transaction processing)
of an organization and will require majority voting.

* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" with <version>
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network <id>
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"
* From "Node1" suspend org "ORG1", confirm that org status is "PendingSuspension"
* From "Node1" approve org "ORG1"'s suspension, confirm that org status is "Suspended"
* Deploy "storec" smart contract with initial value "5" from a default account in "Node4" fails with error "read only account. cannot transact"
* From "Node1" revoke suspension of org "ORG1", confirm that org status is "RevokeSuspension"
* From "Node1" approve org "ORG1"'s suspension revoke, confirm that org status is "Approved"
* Deploy "storec" smart contract with initial value "5" from a default account in "Node4", named this contract as "c1"
* "c1"'s "getc" function execution in "Node4" returns "5"

## Deactivating an organization to stop block synchronization in a node

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Validate that org "NWADMIN" is approved, has "Node1" linked and has role "NWADMIN"
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active
* From "Node1" propose and approve new org "ORG1" with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" with <version>
* Write "permissionsConfig" to the data directory of "Node4"
* Restart network <id>
* Validate that org "ORG1" is approved, has "Node4" linked and has role "ORGADMIN"
* From "Node1" deactivate org "NWADMIN"'s node "Node3"
* Check org "NWADMIN" has "Node3" with status "Deactivated"
* Save current blocknumber from "Node3"
* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c2"
* Deploy "storec" smart contract with initial value "1" from a default account in "Node1", named this contract as "c3"
* Ensure current blocknumber from "Node3" has not changed

