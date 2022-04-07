# This network is to enable MPS without changing how client interacts with the network
# This represents a typical network upgrade scenario where a node (Node0) is setup to
# enable MPS

consensus = "qbft"

#Enable MPS but configure only 1 private state

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
  0 = ["Key1"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
  0 = ["EthKey1"]
}

# sets up the MPS allocations
# Quorum instance 1 has the name "Node5" as the default is "Node1" and would clash with the allocation in Quorum instance 0
override_vnodes = {
  0 = {
    mpsEnabled = true,
    vnodes = {
      VNode1 = {
        name    = "Node1"
        tmKeys  = ["Key1"],
        ethKeys = ["EthKey1"]
      }
    }
  }
}
