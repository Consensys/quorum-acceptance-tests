#
#  This network sets up a 6 node qlight network where the atests will execute against ql server nodes
#  node 1 ql server
#  node 2 standard node
#  node 3 standard node
#  node 4 ql server
#  node 5 ql client (server = node 1)
#  node 6 ql client (server = node 4)
#

number_of_nodes     = 6
consensus           = "istanbul"
qbftBlock           = { block = 0, enabled = true }
quorum_docker_image = { name = "chounsom/qrm:develop", local = false }

qlight_clients = {
    4 = { ql_server_idx = 0 },
    5 = { ql_server_idx = 3 }
}

qlight_server_indices = [0, 3]
