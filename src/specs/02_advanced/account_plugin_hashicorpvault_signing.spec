# Pluggable Architecture with Hashicorp Vault implementation of account plugin

 Tags: networks/plugins::raft-account-plugin-hashicorp-vault, networks/plugins::istanbul-account-plugin-hashicorp-vault, networks/plugins::qbft-account-plugin-hashicorp-vault, plugin-account, hashicorp-vault, signing

* Add Hashicorp Vault account 0x6038dc01869425004ca0b8370f6c81cf464213b3 to "Node1" with secret engine path "kv" and secret path "myacct"
* "Node1" has account 0x6038dc01869425004ca0b8370f6c81cf464213b3

## Hashicorp Vault accounts correctly sign transactions
* "Node1" gets the expected result when signing a known transaction with account 0x6038dc01869425004ca0b8370f6c81cf464213b3

## Hashicorp Vault accounts correctly sign arbitrary data
* "Node1" gets the expected result when signing known arbitrary data with account 0x6038dc01869425004ca0b8370f6c81cf464213b3

