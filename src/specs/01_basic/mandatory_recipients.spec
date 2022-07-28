# Validate a subset of recipients that are mandatory for a specific private contract

 Tags: mandatory-recipients

Private smart contract can have a subset of participants that are mandated to be included in all transactions
for a particular contract

## Transactions sent with invalid mandatory recipients data will be rejected

* Deploy a "StandardPrivate" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node3" and mandatory for "Node3" fails with message "privacy metadata invalid. mandatory recipients are only applicable for PrivacyFlag=2(MandatoryRecipients)"
* Deploy a "PartyProtection" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node3" and mandatory for "Node3" fails with message "privacy metadata invalid. mandatory recipients are only applicable for PrivacyFlag=2(MandatoryRecipients)"
* Deploy a "StateValidation" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node3" and mandatory for "Node3" fails with message "privacy metadata invalid. mandatory recipients are only applicable for PrivacyFlag=2(MandatoryRecipients)"
* Deploy a MandatoryRecipients contract `SimpleStorage` with no mandatory recipients data fails with message "missing mandatory recipients data. if no mandatory recipients required consider using PrivacyFlag=1(PartyProtection)"

## Transactions sent with mandatory recipients not included in participant list will be rejected

* Deploy a "MandatoryRecipients" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node3" and mandatory for "Node4" fails with message "mandatory recipients not included in the participant list"
* Deploy a "MandatoryRecipients" contract `SimpleStorage` with initial value "42" in "Node1"'s default account and it's private for "Node2,Node3" and mandatory for "Node3,Node4" fails with message "mandatory recipients not included in the participant list"

## The mandatory recipients will have the full view of the private state of the contract

* Deploy a "MandatoryRecipients" contract `SimpleStorage` with initial value "30" in "Node1"'s default account and it's private for "Node2" and mandatory for "Node2", named this contract as "contract12_2"
* Fail to execute "MandatoryRecipients" simple contract("contract12_2")'s `set()` function with new arbitrary value in "Node1" and it's private for "Node1" and mandatory for "Node1" with error "Privacy metadata mismatched"
* Fire and forget execution of "MandatoryRecipients" simple contract("contract12_2")'s `set()` function with new value "19" in "Node1" and it's private for "Node2" and mandatory for "Node2"
* Fire and forget execution of "MandatoryRecipients" simple contract("contract12_2")'s `set()` function with new value "10" in "Node2" and it's private for "Node2" and mandatory for "Node2"
* Execute "contract12_2"'s `get()` function in "Node1" returns "19"
* Execute "contract12_2"'s `get()` function in "Node2" returns "10"
* Deploy a "MandatoryRecipients" contract `SimpleStorage` with initial value "30" in "Node2"'s default account and it's private for "Node3,Node4" and mandatory for "Node2", named this contract as "contract234_2"
* Fail to execute "MandatoryRecipients" simple contract("contract234_2")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node4" and no mandatory recipients with error "missing mandatory recipients data. if no mandatory recipients required consider using PrivacyFlag=1(PartyProtection)"
* Fail to execute "MandatoryRecipients" simple contract("contract234_2")'s `set()` function with new arbitrary value in "Node4" and it's private for "Node3" and no mandatory recipients with error "missing mandatory recipients data. if no mandatory recipients required consider using PrivacyFlag=1(PartyProtection)"
* Fail to execute "MandatoryRecipients" simple contract("contract234_2")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node4" and mandatory for "Node3" with error "Privacy metadata mismatched"
* Fail to execute "MandatoryRecipients" simple contract("contract234_2")'s `set()` function with new arbitrary value in "Node4" and it's private for "Node3" and mandatory for "Node4" with error "Privacy metadata mismatched"
* Fire and forget execution of "MandatoryRecipients" simple contract("contract234_2")'s `set()` function with new value "10" in "Node2" and it's private for "Node2" and mandatory for "Node2"
* Execute "contract234_2"'s `get()` function in "Node2" returns "10"
* Execute "contract234_2"'s `get()` function in "Node3" returns "30"
* Execute "contract234_2"'s `get()` function in "Node4" returns "30"

## Mandatory recipients are validated in nested contracts

* Deploy a MR contract `C1` with initial value "42" in "Node1"'s default account and it's private for "Node2", mandatory for "Node2" named this contract as "contract12_2"
* "contract12_2" is deployed "successfully" in "Node1,Node2"
* Deploy a MR contract `C2` with initial value "contract12_2" in "Node2"'s default account and it's private for "Node3" mandatory for "Node3", named this contract as "contract23_3"
* "contract23_3" is deployed "successfully" in "Node2,Node3"
The next step should fail as part of Party Protection check - Node3 was not a participant of contract C1 - which the transaction was going to affect
* Fail to execute "MandatoryRecipients" simple contract("contract23_3")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node2" and mandatory for "Node3" with error "JsonRpcError thrown with code -32000. Message: execution reverted"
This step should fail the mandatory validations
* Fail to execute "MandatoryRecipients" simple contract("contract23_3")'s `set()` function with new arbitrary value in "Node2" and it's private for "Node3" and mandatory for "Node3" with error "Privacy metadata mismatched"
* Fire and forget execution of "MandatoryRecipients" simple contract("contract23_3")'s `set()` function with new value "10" in "Node2" and it's private for "Node3" and mandatory for "Node2,Node3"
* Execute "contract23_3"'s `get()` function in "Node2" returns "10"
* Execute "contract12_2"'s `get()` function in "Node1" returns "42"

## Contract with mandatory recipients can be extended
Failure to execute transaction with invalid mandatory recipients on the extended node proves that it has the relevant
mandatory recipients data to perform validations.
* Deploy a "MandatoryRecipients" contract `SimpleStorage` with initial value "30" in "Node1"'s default account and it's private for "Node2" and mandatory for "Node1", named this contract as "contract12_1_3"
* Initiate "contract12_1_3" extension from "Node1" to "Node3". Contract extension accepted in receiving node. Check that state value in receiving node is "30"
* Fail to execute "MandatoryRecipients" simple contract("contract12_1_3")'s `set()` function with new arbitrary value in "Node3" and it's private for "Node2" and mandatory for "Node3" with error "Privacy metadata mismatched"
* Fire and forget execution of "MandatoryRecipients" simple contract("contract12_1_3")'s `set()` function with new value "10" in "Node3" and it's private for "Node1" and mandatory for "Node1"
* Execute "contract12_1_3"'s `get()` function in "Node1" returns "10"
* Execute "contract12_1_3"'s `get()` function in "Node3" returns "10"
* Execute "contract12_1_3"'s `get()` function in "Node2" returns "30"
