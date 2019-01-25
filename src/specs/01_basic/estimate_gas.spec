# Estimate gas required for transactions and contracts

 Tags: basic, estimategas

EstimateGas api call should return valid 'close' estimate of required gas.

## Estimate gas required for public transaction

 Tags: public

* Estimate gas for public transaction transferring some Wei from a default account in "Node1" to a default account in "Node2"
* Gas estimate "21000" is returned within "10" percent


## Deploy public smart contract, this is used for estimating the calls (we also need it so we can use the binary data in the estimateGas() acceptance tests below)

 Tags: public

* Deploy `SimpleContract` public smart contract from a default account in "Node1"

## Estimate gas required to create public smart contract

 Tags: public

* Estimate gas for deploying `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "83586" is returned within "10" percent

## Estimate gas required to call a public smart contract

 Tags: public

* Estimate gas for calling the `SimpleContract` public smart contract from a default account in "Node1"
* Gas estimate "41639" is returned within "10" percent


## Deploy private smart contract, this is used for estimating the calls (we also need it so we can use the binary data in the estimateGas() acceptance tests below)

 Tags: private

* Deploy `SimpleContract` private smart contract from a default account in "Node1" and private for "Node4"

## Estimate gas required to create private smart contract

 Tags: private

* Estimate gas for deploying `SimpleContract` private smart contract from a default account in "Node1" and private for "Node4"
* Gas estimate "83586" is returned within "10" percent

## Estimate gas required to call a private smart contract

 Tags: private

* Estimate gas for calling the `SimpleContract` private smart contract from a default account in "Node1" and private for "Node4"
* Gas estimate "41639" is returned within "10" percent
