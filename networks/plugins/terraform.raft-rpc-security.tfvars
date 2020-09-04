consensus    = "raft"
network_name = "plugins-raft"
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

# this is for multi tenancy tests. It must not impact the RPC security runs
override_tm_named_key_allocation = {
  0 = ["A1", "A2", "B1", "C1"]
  1 = ["B2", "D1"]
  2 = ["C2"]
}