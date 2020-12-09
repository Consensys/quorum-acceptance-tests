consensus    = "raft"
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

enable_multitenancy = true

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
  0 = ["A1", "A2", "B1", "B3", "C1"]
  1 = ["B2", "D1"]
  2 = ["C2"]
}

override_named_account_allocation = {
  0 = ["ACC1", "ACC2", "ACC3"]
  1 = ["ACC4", "ACC5", "ACC6"]
  2 = ["ACC7", "ACC8", "ACC9"]
}