# Estimate gas required for transactions and contracts

 Tags: basic, estimategas, private

* Deploy a simple smart contract with initial value "0" in "Node1"'s default account and it's private for "Node4", named this contract as "privateContract1"

EstimateGas api call should return valid 'close' estimate of required gas.

## Estimate gas required to create private smart contract

* Estimate gas for deploying `SimpleContract` private smart contract from a default account in "Node1" and private for "Node4"
* Gas estimate "105921" is returned within "10" percent

## Estimate gas required to call a private smart contract

* Estimate gas for calling the `SimpleContract` private smart contract from a default account in "Node1" and private for "Node4"
* Gas estimate "45261" is returned within "10" percent
* Update contract "privateContract1" with value "99", from "Node1" to "Node4" using estimated gas
