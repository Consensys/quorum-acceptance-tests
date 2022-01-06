# Multiple private raw smart contracts between nodes signed externally

 Tags: basic, spam, raw

## Send transactions from one node to others

* Send "20" simple private raw smart contracts from wallet "Wallet1" in "Node1" and it's separately private for "Node2,Node3" and saved under "contractsData1"
* Send "20" simple private raw smart contracts from wallet "Wallet2" in "Node2" and it's separately private for "Node1,Node3" and saved under "contractsData2"
* Send "20" simple private raw smart contracts from wallet "Wallet8" in "Node3" and it's separately private for "Node1,Node2" and saved under "contractsData3"
* Verify the stored data "contractsData1" into the store to have the right value
* Verify the stored data "contractsData2" into the store to have the right value
* Verify the stored data "contractsData3" into the store to have the right value
