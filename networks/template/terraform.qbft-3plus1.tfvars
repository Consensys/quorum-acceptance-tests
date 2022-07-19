#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes       = 4
exclude_initial_nodes = [3]
consensus             = "qbft"
addtional_geth_args   = "--allow-insecure-unlock"

qbft_empty_block_period = { block = 120, emptyblockperiod = 1 }
transition_config = { transitions: [] }

