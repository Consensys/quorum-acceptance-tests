#
#  This network setups a hybrid network template with 2 quorum and 2 besu nodes and excludes 2nd besu node
#

// Exclude 2nd besu node. Using exclude_initial_nodes for simplicity, it is only used to evaluate istanbul_validators
exclude_initial_besu_nodes = [1]
exclude_initial_nodes      = [3]
number_of_quorum_nodes     = 2
number_of_besu_nodes       = 2
consensus                  = "qbft"
start_quorum               = false
start_besu                 = false
start_tessera              = false
template_network           = true
