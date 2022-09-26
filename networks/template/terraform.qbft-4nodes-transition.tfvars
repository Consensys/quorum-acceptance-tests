#
#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes         = 4
consensus               = "istanbul"
addtional_geth_args     = "--allow-insecure-unlock"
qbftBlock               = { block = 100, enabled = true }
transition_config       = { transitions: [{ "block": 100, "algorithm": "qbft" }, { "block": 110, "miningBeneficiary": quorum_bootstrap_keystore.accountkeys-generator[0].account[0].address, "blockReward": 20}, { "block": 120, "emptyblockperiodseconds": 2, "beneficiaryMode": "list", "beneficiaryList": [quorum_bootstrap_keystore.accountkeys-generator[0].account[0].address, quorum_bootstrap_keystore.accountkeys-generator[1].account[0].address], "blockReward": 10}, { "block": 250, "emptyblockperiodseconds": 1 }, { "block": 251, "beneficiaryMode": "validators" }] }
