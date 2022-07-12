#
#  This network sets up a 5 node qlight + multitenancy network where the atests will execute against an mt + ql *server* node
#  node 1 ql server + mt node (PSIs=[JPM, GS])
#  node 2 standard node
#  node 3 standard node
#  node 4 standard node
#  node 5 ql client (server = node 1, psi JPM)
#

number_of_nodes      = 5
consensus            = "qbft"
privacy_enhancements = { block = 0, enabled = true }

plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

# only want mt on node 1
override_plugins = {
  1 = {},
  2 = {},
  3 = {},
  4 = {},
}

# globally set --multitenancy for all nodes
# this can be overriden by override_additional_geth_args
enable_multitenancy           = true
# only want mt on node 1
override_additional_geth_args = {
  1 = "--allow-insecure-unlock",
  2 = "--allow-insecure-unlock",
  3 = "--allow-insecure-unlock",
  4 = "--allow-insecure-unlock --qlight.client.rpc.tls --qlight.client.rpc.tls.insecureskipverify",
}

qlight_clients = {
  4 = {
    server_idx = 0,
    mps_psi = "JPM",
    mt_is_server_tls_enabled = true,
    mt_scope = "p2p://qlight rpc://eth_* rpc://admin_* rpc://personal_* rpc://quorumExtension_* rpc://rpc_modules psi://JPM?self.eoa=0x0"
  },
}

qlight_server_indices = [0]

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
  0 = ["JPM_K1", "JPM_K2", "GS_K1", "GS_K2"]
  1 = ["DB_K1"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
  0 = ["JPM_ACC1", "JPM_ACC2", "GS_ACC1", "GS_ACC2"]
  1 = ["DB_ACC1"]
  4 = ["JPM_ACC1", "JPM_ACC2"]
}

# this is config will be merged to the default genesis "config" section per node
additional_genesis_config = {
  0 = {
    isMPS = true
  }
}

# this is config will be merged to the default Tessera JSON config per node
additional_tessera_config = {
  0 = {
    "features" : {
      "enablePrivacyEnhancements" : "true",
      "enableRemoteKeyValidation" : "true",
      "enableMultiplePrivateStates" : "true"
    },
    residentGroups = [
      {
        name        = "JPM"
        description = "JPM group"
        members     = [
          "$${JPM_K1}", "$${JPM_K2}"
        ]
      },
      {
        name        = "GS"
        description = "GS group"
        members     = [
          "$${GS_K1}", "$${GS_K2}"
        ]
      },
    ]
  }
}
