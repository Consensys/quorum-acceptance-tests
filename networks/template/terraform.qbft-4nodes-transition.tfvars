#
#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes         = 4
consensus               = "istanbul"
addtional_geth_args     = "--allow-insecure-unlock"
qbftBlock               = { block = 100, enabled = true }
transition_config       = { transitions: [{ "block": 100, "algorithm": "qbft" }, { "block": 110, "miningBeneficiary": "0x0638e1574728b6d862dd5d3a3e0942c3be47d996", "blockReward": "20"}, { "block": 115, "beneficiaryMode": "fixed"}, { "block": 120, "emptyblockperiodseconds": 2, "beneficiaryMode": "list", "beneficiaryList": ["0x0638e1574728b6d862dd5d3a3e0942c3be47d996", "0x9186eb3d20cbd1f5f992a950d808c4495153abd5"], "blockReward": "0xa"}, { "block": 250, "emptyblockperiodseconds": 1, "beneficiaryMode": "validators" }] }
