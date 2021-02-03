consensus    = "raft"

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
    0 = ["Node1", "Node2", "Node3"]
    1 = ["Node4"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
    0 = ["Node1", "Node2", "Node3"]
    1 = ["Node4"]
}
