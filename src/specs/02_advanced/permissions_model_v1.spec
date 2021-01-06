# Enhanced Basic(v1) Permissions Model

 Tags: networks/template::raft-3plus1, pre-condition/no-record-blocknumber, permissions-v1

The Enhanced Permissions Model caters for enterprise-level needs by having a smart contract-based permissioning model.
This allows for significant flexibility to manage nodes, accounts, and account-level access controls.

For more information, visit https://docs.goquorum.com/en/latest/Permissioning/Enhanced%20Permissions%20Model/Overview/

The test scenarios are based on the following setup:

      |id        |networkType      |permissionVersion |qip714block |
      |r1        |permissioned     |v1                |20          |

* Start permission network <networkType> <permissionVersion> <id> <qip714block>

## Network Admin Organization and Role are initialized successfully

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

Network Admin Organization is the name of initial organization that will be created as a part of network boot up with new permissions model.
This organization will own all the initial nodes which come at the time of network boot up and accounts
which will be the network admin account.

Network Admin Role will have full access and will be network admin.
This role will be assigned to the network admin accounts
* Validate network admin role and basic testing

## Proposing a new organization into the network

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

Once the network is up, the network admin accounts can then propose a new organization into the network.
Majority approval from the network admin accounts is required before an organization is approved.

* Propose new organization to the network <permissionVersion> <id>

## Suspending an organization temporarily

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

The network admin accounts can suspend/revoke-suspension all activities (e.g.: transaction processing)
of an organization and will require majority voting.

* Suspend an org <permissionVersion> <id>

## Deactivating an organization to stop block synchronization in a node

 Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Deactivate an org <permissionVersion> <id>

