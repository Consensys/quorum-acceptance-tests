#
#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes     = 4
consensus           = "istanbul"
addtional_geth_args = "--allow-insecure-unlock"
qbftBlock           = { block = 100, enabled = true }
