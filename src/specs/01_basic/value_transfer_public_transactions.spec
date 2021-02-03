# Value transfer in public transactions

 Tags: basic

Value-transfer public transactions are supported by default in Ethereum and Quorum keeps this feature.

## Successful public transaction submission

 Tags: public

* Send "10" Wei from a default account in "Node1" to a default account in "Node4" in a public transaction
* Transaction is accepted in the blockchain
* In "Node1", the default account's balance is now less than its previous balance
* In "Node4", the default account's balance is now greater than its previous balance

## Successful signed public transaction submission

 Tags: public, sign

Signing the transaction before submitting it to the node

* Send "10" Wei from a default account in "Node3" to a default account in "Node4" in a signed public transaction
* Transaction is accepted in the blockchain
* In "Node3", the default account's balance is now less than its previous balance
* In "Node4", the default account's balance is now greater than its previous balance
