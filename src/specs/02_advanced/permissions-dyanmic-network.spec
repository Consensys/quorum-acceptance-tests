# Quorum Permissions model testing with dynamic network

  Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, permissions

## Deploy permissions contracts
* Start a "permissioned" Quorum Network, named it "default", consisting of "Node1,Node2,Node3" with "full" `gcmode`

* Deploy "PermissionsUpgradable" smart contract from a default account in "Node1", name this contract as "upgradable"
* From "Node1" deploy "AccountManager" contract passing "upgradable" address, name it "accountmgr"
* From "Node1" deploy "OrgManager" contract passing "upgradable" address, name it "orgmgr"
* From "Node1" deploy "RoleManager" contract passing "upgradable" address, name it "rolemgr"
* From "Node1" deploy "NodeManager" contract passing "upgradable" address, name it "nodemgr"
* From "Node1" deploy "VoterManager" contract passing "upgradable" address, name it "votermgr"
* From "Node1" deploy "PermissionsInterface" contract passing "upgradable" address, name it "interface"

## Deploy permissions implementation contract which requires address of all contracts
* From "Node1" deploy implementation contract passing addresses of "upgradable", "orgmgr", "rolemgr", "accountmgr", "votermgr", "nodemgr". Name this as "implementation"

## Create permissions-config.json file
* Create permissions-config.json object using the contract addersses of "upgradable", "interface", "implementation", "orgmgr", "rolemgr",  "accountmgr", "votermgr", "nodemgr". Name it "permissionsConfig"
* Update "permissionsConfig". Add "Node1"'s default account to accounts in config
* Update "permissionsConfig". Add "NWADMIN" as network admin org, "NWADMIN" network admin role, "ORGADMIN" as the org admin role
* Update "permissionsConfig". Set suborg depth as "4", suborg breadth as "4"
* Write "permissionsConfig" to the data directory of "Node1,Node2,Node3"

## Execute permissions init
* From "Node1" execute permissions init on "upgradable" passing "interface" and "implementation" contract addresses

## Restart the network
* Stop all nodes in the network "default"
* Start all nodes in the network "default"

## Check the initial status of network after boot up
* Get netowrk details from "Node1"
* Check org "NWADMIN" is "Approved" with no parent, level "1" and empty sub orgs
* Check org "NWADMIN" has "Node1" with status "Approved"
* Check org "NWADMIN" has "Node2" with status "Approved"
* Check org "NWADMIN" has "Node3" with status "Approved"
* Check org "NWADMIN" has role "NWADMIN" with access "FullAccess" and permission to vote and is active
* Check "Node1"'s default account is from org "NWADMIN" and has role "NWADMIN" and is org admin and is active

## Propose a new org, approve the org and bring up the node
* From "Node1" propose new org "ORG1" into the network with "Node4"'s enode id and "Default" account
* From "Node1" approve new org "ORG1" into the network with "Node4"'s enode id and "Default" account
* Start stand alone "Node4" in "networkId"
* Write "permissionsConfig" to the data directory of "Node4"

## Restart the network again
* Stop all nodes in the network "default"
* Start all nodes in the network "default"
* Get netowrk details from "Node4"
* Check org "ORG1" is "Approved" with no parent, level "1" and empty sub orgs
* Check org "ORG1" has "Node4" with status "Approved"
* Check org "ORG1" has role "ORGADMIN" with access "FullAccess" and permission to vote and is active

