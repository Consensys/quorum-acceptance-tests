# Estimate gas required for transactions and contracts

 Tags: basic

EstimateGas api call should return valid 'close' estimate of required gas.

## Estimate gas required for public transaction

 Tags: public, nosupport

* Estimate gas for public transaction transferring some Wei from a default account in "Node1" to a default account in "Node2"
* Gas estimate "21000" is returned


## Deploy public smart contract, this is used for estimating the calls (we also need it so we can use the binary data in the estimateGas() acceptance tests below)

* Deploy `SimpleContract` public smart contract from a default account in "Node1"

## Estimate gas required to create public smart contract

 Tags: public, nosupport

* Estimate gas for deploying `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "77841" is returned

## Estimate gas required to call a public smart contract

 Tags: public, nosupport

* Estimate gas for calling the `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "21464" is returned


## Deploy private smart contract, this is used for estimating the calls (we also need it so we can use the binary data in the estimateGas() acceptance tests below)

* Deploy `SimpleContract` private smart contract from a default account in "Node1" and private for "Node7"

## Estimate gas required to create private smart contract

 Tags: private, nosupport

* Estimate gas for deploying `SimpleContract` private smart contract from a default account in "Node1" and private for "Node7"
* Gas estimate "77841" is returned

## Estimate gas required to call a private smart contract

 Tags: public, nosupport

* Estimate gas for calling the `SimpleContract` private smart contract from a default account in "Node1" and private for "Node7"
* Gas estimate "21464" is returned
