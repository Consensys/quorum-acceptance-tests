#
#  This network sets up a 6 node qlight network where the atests will execute against ql *server* nodes
#  node 1 ql server standard node
#  node 2 standard node
#  node 3 standard node
#  node 4 ql server standard node
#  node 5 ql client (server = node 1)
#  node 6 ql client (server = node 4)
#

number_of_nodes     = 6
consensus           = "qbft"

qlight_clients = {
    4 = { server_idx = 0, mps_psi = "", mt_is_server_tls_enabled = false,  mt_scope = "" },
    5 = { server_idx = 3, mps_psi = "", mt_is_server_tls_enabled = false,  mt_scope = "" }
}

qlight_server_indices = [0, 3]

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
    0 = ["TmKey0"]
    1 = ["TmKey1"]
    2 = ["TmKey2"]
    3 = ["TmKey3"]
    4 = ["TmKey0"]
    5 = ["TmKey3"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
    0 = ["EthKey0"]
    1 = ["EthKey1"]
    2 = ["EthKey2"]
    3 = ["EthKey3"]
    4 = ["EthKey0"]
    5 = ["EthKey3"]
}

override_vnodes = {
    "0" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node1"
                ethKeys = ["EthKey0"]
                "tmKeys" = ["TmKey0"]
            }
        }
    },
    "1" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node2"
                ethKeys = ["EthKey1"]
                "tmKeys" = ["TmKey1"]
            }
        }
    },
    "2" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node3"
                ethKeys = ["EthKey2"]
                "tmKeys" = ["TmKey2"]
            }
        }
    },
    "3" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node4"
                ethKeys = ["EthKey3"]
                "tmKeys" = ["TmKey3"]
            }
        }
    },
    "4" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node5"
                ethKeys = ["EthKey0"]
                "tmKeys" = ["TmKey0"]
            }
        }
    },
    "5" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node6"
                ethKeys = ["EthKey3"]
                "tmKeys" = ["TmKey3"]
            }
        }
    }
}
