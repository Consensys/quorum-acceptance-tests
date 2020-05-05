# Pluggable Architecture with Hashicorp Vault implementation of account plugin

 Tags: networks/plugins::raft, networks/plugins::istanbul, plugin-account, hashicorp-vault, account-creation

* Delete all files in "Node1"'s account config directory

## New account can be created in Hashicorp Vault using RPC API

* Calling `plugin@account_newAccount` API in "Node1" with single parameter "{\"secretEnginePath\": \"kv\",\"secretPath\": \"myacct\",\"insecureSkipCAS\": true}" returns the new account address and Vault secret URL
* The account address and a private key exist at the Vault secret URL
* File is created in "Node1"'s account config directory


## Account can be imported in to Hashicorp Vault using RPC API
* Calling `plugin@account_importRawKey` API in "Node1" with parameters "1fe8f1ad4053326db20529257ac9401f2e6c769ef1d736b8c2f5aba5f787c72b" and "{\"secretEnginePath\": \"kv\",\"secretPath\": \"myacct\",\"insecureSkipCAS\": true}" returns the imported account address and Vault secret URL
* The account address and private key "1fe8f1ad4053326db20529257ac9401f2e6c769ef1d736b8c2f5aba5f787c72b" exist at the Vault secret URL
* File is created in "Node1"'s account config directory

___
* Delete all files in "Node1"'s account config directory
