# Party protection and PSV transactions are rejected by quorum when privacy enhancements are disabled

 Tags: basic, privacy, privacy-enhancements-disabled

## Deploying simple contract with privacyFlag PartyProtection fails

 Tags: single

* Deploying a "PartyProtection" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4" fails with message "PrivacyEnhancements are disabled"

## Sending a PartyProtection transaction to a StandardPrivate contract fails

 Tags: single

* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "PartyProtection" simple contract("contract14")'s `set()` function with new arbitrary value in "Node4" and it's private for "Node1" with error "PrivacyEnhancements are disabled."

## Deploying simple contract with privacyFlag StateValidation fails

 Tags: single

* Deploying a "StateValidation" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4" fails with message "PrivacyEnhancements are disabled"

## Sending a StateValidation transaction to a StandardPrivate contract fails

 Tags: single

* Deploy a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract14"
* "contract14" is deployed "successfully" in "Node1,Node4"
* Fail to execute "StateValidation" simple contract("contract14")'s `set()` function with new arbitrary value in "Node4" and it's private for "Node1" with error "PrivacyEnhancements are disabled."

