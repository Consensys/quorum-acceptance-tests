# Check gas usage when gas price is enabled

 Tags: gas-price-enabled

If gas price is enabled then correct amount should be deducted from 'from' account and credited to minter.

## Public contract gas cost should be debited from sender account and credited to miner.

* Public transaction from non-minter where gas value is "200000", name this contract as "contract1"
* Contract "contract1" creation succeeded
* On source node, the default account's balance is now less than its previous balance
* On minter, the default account's balance is now greater than its previous balance

## Private contract gas cost should be debited from sender account and credited to miner.

Note that private transaction cost is only intrinsic gas.
If privacy marker transactions are enabled then the PMT will also only use minimal gas.

* Private transaction from non-minter where gas value is "140000", name this contract as "contract1"
* Contract "contract1" creation succeeded
* On source node, the default account's balance is now less than its previous balance
* On minter, the default account's balance is now greater than its previous balance
