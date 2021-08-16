# Pluggable Architecture with Hashicorp Vault implementation of account plugin

 Tags: networks/plugins::raft-account-plugin-hashicorp-vault, networks/plugins::istanbul-account-plugin-hashicorp-vault, networks/plugins::qbft-account-plugin-hashicorp-vault, plugin-account, hashicorp-vault, account-creation

## New account can be created in Hashicorp Vault using RPC API

* Calling `plugin@account_newAccount` API in "Node1" with single parameter "{\"secretName\": \"myacct\", \"overwriteProtection\": {\"insecureDisable\": true}}}" returns the new account address and Vault secret URL
* The account address and a private key exist at kv secret engine with name "kv" and secret name "myacct"

## Account can be imported in to Hashicorp Vault using RPC API
* Calling `plugin@account_importRawKey` API in "Node1" with parameters "1fe8f1ad4053326db20529257ac9401f2e6c769ef1d736b8c2f5aba5f787c72b" and "{\"secretName\": \"myacct\", \"overwriteProtection\": {\"insecureDisable\": true}}" returns the imported account address and Vault secret URL
* The account address and private key "1fe8f1ad4053326db20529257ac9401f2e6c769ef1d736b8c2f5aba5f787c72b" exist at kv secret engine with name "kv" and secret name "myacct"
