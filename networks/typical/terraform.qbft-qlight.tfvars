#
#  This network sets up a 6 node qlight network - 4 standard nodes (2 ql server, 2 not ql server) and 2 ql client nodes
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
