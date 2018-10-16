# private signed transaction - value transfer - run transactions in sequence

This spec covers testing ether value transfer from one account to another account.
If the transaction is successful it should get mined successfully.
Checks are done to ensure the balances are changing in the from account and to account of a transaction and blocks are gettting
created.


## test transferring random ether value from a default account in node1 to a default account in nodes 2,3,4,5,6 & 7
* send signed private transaction from node "1" to all other nodes one by one

## test transferring random ether value from a default account in node2 to a default account in nodes 1,3,4,5,6 & 7
* send signed private transaction from node "2" to all other nodes one by one

## test transferring random ether value from a default account in node3 to a default account in nodes 1,2,4,5,6 & 7
* send signed private transaction from node "3" to all other nodes one by one

## test transferring random ether value from a default account in node4 to a default account in nodes 1,2,3,5,6 & 7
* send signed private transaction from node "4" to all other nodes one by one

## test transferring random ether value from a default account in node5 to a default account in nodes 1,2,3,4,6 & 7
* send signed private transaction from node "5" to all other nodes one by one

## test transferring random ether value from a default account in node6 to a default account in nodes 1,2,3,4,5 & 7
* send signed private transaction from node "6" to all other nodes one by one

## test transferring random ether value from a default account in node7 to a default account in nodes 1,2,3,4,5 & 6
* send signed private transaction from node "7" to all other nodes one by one

