#
#  This network setups with 3 nodes and has 4th node as an extra node
#

number_of_nodes     = 4
consensus           = "istanbul"
qbftBlock           = { block = 0, enabled = true }
quorum_docker_image = { name = "jbhurat/quorum:qbft", local = false }
