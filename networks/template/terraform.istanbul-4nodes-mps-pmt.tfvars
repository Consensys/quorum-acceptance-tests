#
#  This network setups with 4 nodes
#

number_of_nodes = 4
consensus       = "istanbul"
# Import images so they can be used programatically in the test
docker_images   = []

privacy_enhancements = { block = 0, enabled = true }
privacy_precompile={ block = 0, enabled = true}
privacy_marker_transactions=true

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
    0 = ["Key1", "Key2", "Key3"]
    1 = ["Key4"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
    0 = ["EthKey1", "EthKey2", "EthKey3"]
    1 = ["EthKey4"]
}

# sets up the MPS allocations
# Quorum instance 1 has the name "Node5" as the default is "Node1" and would clash with the allocation in Quorum instance 0
override_vnodes = {
    0 = {
        mpsEnabled = true,
        vnodes = {
            VNode1 = {
                name = "Node1"
                tmKeys = ["Key1", "Key2"],
                ethKeys = ["EthKey1", "EthKey2"]
            },
            VNode2 = {
                name = "Node2"
                tmKeys = ["Key3"],
                ethKeys = ["EthKey3"]
            }
        }
    },
    1 = {
        mpsEnabled = false,
        vnodes = {
            VNode3 = {
                name = "Node5"
                tmKeys = ["Key4"],
                ethKeys = ["EthKey4"]
            }
        }
    }
}
