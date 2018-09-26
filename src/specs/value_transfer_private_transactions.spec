# Value transfer in private transactions

Quorum does not support private transactions which transfer value.

## Unsuccessful private transaction submission

tags: privacy, nosupport

* Send some ETH from a default account in "Node1" to a default account in "Node2" in a private transaction.
* Error message "ether value is not supported for private transactions" is returned.

## Unsuccessful signed private transaction submission

tags: privacy, nosupport, sign

Signing the transaction before submitting it to the node

* Send some ETH from a default account in "Node3" to a default account in "Node4" in a signed private transaction.
* Error message "ether value is not supported for private transactions" is returned.
