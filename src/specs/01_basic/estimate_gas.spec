# Estimate gas required for transactions and contracts

 Tags: basic, estimategas, public

* Deploy a simple smart contract with initial value "0" in "Node1"'s default account, named this contract as "publicContract1"

EstimateGas api call should return valid 'close' estimate of required gas.

## Estimate gas required for public transaction

* Estimate gas for public transaction transferring some Wei from a default account in "Node1" to a default account in "Node2"
* Gas estimate "21000" is returned within "10" percent

## Estimate gas required to create public smart contract

* Estimate gas for deploying `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "105921" is returned within "10" percent

## Estimate gas required to call a public smart contract

* Estimate gas for calling the `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "45261" is returned within "10" percent

## Estimate gas required for zero value public transaction

* Estimate gas for public transaction transferring zero Wei from a default account in "Node1" to a default account in "Node2"
* Gas estimate "22024" is returned within "10" percent
