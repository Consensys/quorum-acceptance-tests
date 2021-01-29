consensus    = "raft"
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

enable_multitenancy = true
privacy_enhancements = { block = 0, enabled = true }

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
  0 = ["JPM_K1", "JPM_K2", "GS_K1", "GS_K3"]
  1 = ["GS_K2", "DB_K1"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
  0 = ["JPM_ACC1", "JPM_ACC2", "GS_ACC1", "GS_ACC3"]
  1 = ["GS_ACC2", "DB_ACC1"]
}