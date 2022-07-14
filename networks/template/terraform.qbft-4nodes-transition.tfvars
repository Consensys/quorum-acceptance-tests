#
#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes         = 4
consensus               = "istanbul"
addtional_geth_args     = "--allow-insecure-unlock"
qbftBlock               = { block = 100, enabled = true }
qbft_empty_block_period = { block = 120, emptyblockperiod = 5 }
transition_config = { transitions: [{ "block": tonumber(var.qbftBlock.block), "algorithm": "qbft" }, { "block": tonumber(var.qbft_empty_block_period.block), "emptyblockperiodseconds": tonumber(var.qbft_empty_block_period.emptyblockperiod) }, { "block": tonumber(250), "emptyblockperiodseconds": tonumber(1) }] }
