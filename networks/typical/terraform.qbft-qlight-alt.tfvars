#
#  This network sets up an alternate 6 node qlight network where the atests will execute against ql *client* nodes
#  node 1 ql client (server = node 5)
#  node 2 standard node
#  node 3 standard node
#  node 4 ql client (server = node 6)
#  node 5 ql server standard node
#  node 6 ql server standard node
#

number_of_nodes     = 6
consensus           = "qbft"

qlight_clients = {
    0 = { server_idx = 4, mps_psi = "", mt_is_server_tls_enabled = false,  mt_scope = "" },
    3 = { server_idx = 5, mps_psi = "", mt_is_server_tls_enabled = false,  mt_scope = "" }
}

qlight_server_indices = [4, 5]

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
    0 = ["TmKey4"]
    1 = ["TmKey1"]
    2 = ["TmKey2"]
    3 = ["TmKey5"]
    4 = ["TmKey4"]
    5 = ["TmKey5"]
}

# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
    0 = ["EthKey4"]
    1 = ["EthKey1"]
    2 = ["EthKey2"]
    3 = ["EthKey5"]
    4 = ["EthKey4"]
    5 = ["EthKey5"]
}

override_vnodes = {
    "0" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node1"
                ethKeys = ["EthKey4"]
                "tmKeys" = ["TmKey4"]
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
                ethKeys = ["EthKey5"]
                "tmKeys" = ["TmKey5"]
            }
        }
    },
    "4" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node5"
                ethKeys = ["EthKey4"]
                "tmKeys" = ["TmKey4"]
            }
        }
    },
    "5" = {
        mpsEnabled = false
        vnodes = {
            "id" = {
                name = "Node6"
                ethKeys = ["EthKey5"]
                "tmKeys" = ["TmKey5"]
            }
        }
    }
}
