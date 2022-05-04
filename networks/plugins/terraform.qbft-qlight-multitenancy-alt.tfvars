#
#  This network sets up an alternate 5 node qlight + multitenancy network where the atests will execute against an mt + ql *client* node
#  node 1 ql client (server = node 5, psi JPM)
#  node 2 standard node
#  node 3 standard node
#  node 4 standard node
#  node 5 ql server + mt node (PSIs=[JPM, GS])
#

number_of_nodes     = 5
consensus           = "istanbul"
qbftBlock           = { block = 0, enabled = true }
privacy_enhancements = { block = 0, enabled = true }

// TODO(cjh) delete
quorum_docker_image = { name = "quorumengineering/quorum:develop-quiet-qbft", local = true }

plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

# only want mt on node 5
override_plugins = {
  0 = {},
  1 = {},
  2 = {},
  3 = {},
}

# globally set --multitenancy for all nodes
# this can be overriden by override_additional_geth_args
enable_multitenancy = true
# only want mt on node 5
override_additional_geth_args = {
  0 = "--allow-insecure-unlock",
  1 = "--allow-insecure-unlock",
  2 = "--allow-insecure-unlock",
  3 = "--allow-insecure-unlock",
}

qlight_clients = {
  0 = { ql_server_idx = 4, psi = "JPM" },
}

qlight_server_indices = [4]

# this is to setup the TM keys allocation per acceptance tests requirement
override_tm_named_key_allocation = {
  4 = ["JPM_K1", "JPM_K2", "GS_K1", "GS_K2"]
  1 = ["DB_K1"]
}
# this is to setup the Ethereum Accounts allocation per acceptance tests requirement
override_named_account_allocation = {
  4 = ["JPM_ACC1", "JPM_ACC2", "GS_ACC1", "GS_ACC2"]
  1 = ["DB_ACC1"]
  0 = ["JPM_ACC1", "JPM_ACC2"]
}

# this is config will be merged to the default genesis "config" section per node
additional_genesis_config = {
  4 = {
    isMPS = true
  }
}

# this is config will be merged to the default Tessera JSON config per node
additional_tessera_config = {
  4 = {
    "features": {
        "enablePrivacyEnhancements":"true",
        "enableRemoteKeyValidation":"true",
        "enableMultiplePrivateStates" : "true"
    },
    residentGroups = [
      {
        name = "JPM"
        description = "JPM group"
        members = [
          "$${JPM_K1}", "$${JPM_K2}"
        ]
      },
      {
        name = "GS"
        description = "GS group"
        members = [
          "$${GS_K1}", "$${GS_K2}"
        ]
      },
    ]
  }
}
