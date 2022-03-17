#
#  This network sets up an alternate 6 node qlight network where the atests will execute against ql client nodes
#  node 1 ql client (server = node 5)
#  node 2 standard node
#  node 3 standard node
#  node 4 ql client (server = node 6)
#  node 5 ql server
#  node 6 ql server
#

number_of_nodes     = 6
consensus           = "istanbul"
qbftBlock           = { block = 0, enabled = true }
quorum_docker_image = { name = "quorumengineering/quorum:qlight", local = true }

qlight_clients = {
    0 = { ql_server_idx = 4 },
    3 = { ql_server_idx = 5 }
}

qlight_server_indices = [4, 5]
