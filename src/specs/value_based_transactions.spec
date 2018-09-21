# Value-based Transactions

Value-based public transactions are supported by default in Etherum and Quorum keeps this feature.
However, Quorum does not support value-based private transactions.

## Successful public transaction submission
tags: public

* Send "10" ETH from a default account in "Node1" to a default account in "Node2" in a public transaction.
* Transaction is accepted in the blockchain.
* In "Node1", the default account's balance is now "10" lesser than its previous balance.
* In "Node2", the default account's balance is now "10" more than its previous balance.

## Successful signed public transaction submission
tags: public, sign

Signing the transaction before submitting it to the node

* Send "10" ETH from a default account in "Node3" to a default account in "Node4" in a signed public transaction.
* Transaction is accepted in the blockchain.
* In "Node3", the default account's balance is now "10" lesser than its previous balance.
* In "Node4", the default account's balance is now "10" more than its previous balance.

## Unsuccessful private transaction submission
tags: privacy, nosupport

* Send "10" ETH from a default account in "Node1" to a default account in "Node2" in a private transaction.
* An error is returned.

## Unsuccessful signed private transaction submission
tags: privacy, nosupport

Signing the transaction before submitting it to the node

* Send "10" ETH from a default account in "Node3" to a default account in "Node4" in a signed private transaction.
* An error is returned.