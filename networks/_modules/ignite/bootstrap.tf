locals {
  enode_urls = formatlist("\"enode://%s@%s:%d?discport=0&raftport=%d\"", quorum_bootstrap_node_key.nodekeys-generator[*].hex_node_id, var.geth_networking[*].ip.private, var.geth_networking[*].port.p2p, var.geth_networking[*].port.raft)

  # metadata for network subjected to initial participants input
  network = {
    hexNodeIds      = [for idx in local.node_indices : quorum_bootstrap_node_key.nodekeys-generator[idx].hex_node_id if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
    geth_networking = [for idx in local.node_indices : var.geth_networking[idx] if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
    enode_urls      = [for idx in local.node_indices : local.enode_urls[idx] if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
  }

  # chain config
  qip714Block_config              = var.permission_qip714Block.enabled ? { qip714Block = var.permission_qip714Block.block } : {}
  privacyEnhancementsBlock_config = var.privacy_enhancements.enabled ? { privacyEnhancementsBlock = var.privacy_enhancements.block } : {}
  privacyPrecompileBlock_config = var.privacy_precompile.enabled ? { privacyPrecompileBlock = var.privacy_precompile.block } : {}

  istanbul_config       = var.consensus == "istanbul" || var.consensus == "qbft" ? { istanbul = { epoch = 30000, policy = 0, ceil2Nby3Block = 0 } } : {}
  qbft_config  = (var.consensus == "istanbul" || var.consensus == "qbft") && var.qbftBlock.enabled ? { istanbul = { epoch = 30000, policy = 0, testQBFTBlock = var.qbftBlock.block, ceil2Nby3Block = 0 } } : {}
  qbft_istanbul_config = merge(local.istanbul_config, local.qbft_config)

  chain_configs = [for idx in local.node_indices : merge(
    {
      homesteadBlock      = 0
      byzantiumBlock      = 0
      constantinopleBlock = 0
      istanbulBlock       = 0
      petersburgBlock     = 0
      chainId             = var.hybrid_network ? var.hybrid_network_id : random_integer.network_id.result
      eip150Block         = 0
      eip155Block         = 0
      eip150Hash          = "0x0000000000000000000000000000000000000000000000000000000000000000"
      eip158Block         = 0
      isQuorum            = true
      isMPS               = local.vnodes[idx].mpsEnabled
      maxCodeSizeConfig = [
        {
          block = 0
          size  = 80
        }
      ]
    }, local.qip714Block_config, local.privacyEnhancementsBlock_config, local.privacyPrecompileBlock_config, local.qbft_istanbul_config, lookup(var.additional_genesis_config, idx, {}))]
}

data "null_data_source" "meta" {
  count = local.number_of_nodes
  inputs = {
    idx             = count.index
    tmKeys          = join(",", [for k in local.tm_named_keys_alloc[count.index] : element(local.key_data, index(local.tm_named_keys_all, k))])
    nodeUrl         = format("http://%s:%d", var.geth_networking[count.index].ip.public, var.geth_networking[count.index].port.http.external)
    tmThirdpartyUrl = format("http://%s:%d", var.tm_networking[count.index].ip.public, var.tm_networking[count.index].port.thirdparty.external)
    graphqlUrl      = var.geth_networking[count.index].graphql ? format("http://%s:%d/graphql", var.geth_networking[count.index].ip.public, var.geth_networking[count.index].port.http.external) : ""
  }
}

resource "random_integer" "network_id" {
  max = 3000
  min = 1400
}

resource "quorum_bootstrap_network" "this" {
  name       = local.network_name
  target_dir = local.generated_dir
}

resource "quorum_bootstrap_keystore" "accountkeys-generator" {
  count                = local.hybrid_network ? 0 : local.number_of_nodes
  keystore_dir         = format("%s/%s%s/keystore", quorum_bootstrap_network.this.network_dir_abs, local.node_dir_prefix, count.index)
  use_light_weight_kdf = true

  dynamic "account" {
    for_each = lookup(local.named_accounts_alloc, count.index)
    content {
      passphrase = ""
      balance    = "1000000000000000000000000000"
    }
  }
}

resource "quorum_bootstrap_node_key" "nodekeys-generator" {
  count = local.number_of_nodes
}

resource "quorum_transaction_manager_keypair" "tm" {
  count = length(local.tm_named_keys_all)
  config {
    memory = 100000
  }
}

resource "local_file" "tm" {
  count    = length(local.tm_named_keys_all)
  filename = format("%s/%s", local.tmkeys_generated_dir, element(local.tm_named_keys_all, count.index))
  content  = local.key_data[count.index]
}

data "quorum_bootstrap_genesis_mixhash" "this" {
}

resource "quorum_bootstrap_istanbul_extradata" "this" {
  istanbul_addresses = [for idx in local.node_indices : quorum_bootstrap_node_key.nodekeys-generator[idx].istanbul_address if lookup(local.istanbul_validators, idx, "false") == "true"]
  mode               = var.qbftBlock.enabled && var.qbftBlock.block == 0 ? "qbft" : "ibft1"
}

resource "local_file" "genesis-file" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/genesis.json", quorum_bootstrap_network.this.network_dir_abs, local.node_dir_prefix, count.index)
  content  = <<-EOF
{
    "coinbase": "0x0000000000000000000000000000000000000000",
    "config": ${jsonencode(local.chain_configs[count.index])},
    "difficulty": "${var.consensus == "istanbul" || var.consensus == "qbft" ? "0x1" : "0x0"}",
%{if var.hybrid_network~}
    "extraData": "${var.hybrid_extradata}",
%{else~}
    "extraData": "${var.consensus == "istanbul" ? quorum_bootstrap_istanbul_extradata.this.extradata : "0x0000000000000000000000000000000000000000000000000000000000000000"}",
%{endif~}
    "gasLimit": "0xFFFFFF00",
    "mixhash": "${var.consensus == "istanbul" || var.consensus == "qbft" ? data.quorum_bootstrap_genesis_mixhash.this.istanbul : "0x00000000000000000000000000000000000000647572616c65787365646c6578"}",
    "nonce": "0x0",
    "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000",
    "timestamp": "0x00",
%{if var.hybrid_network~}
  "alloc": {
    ${join(",", var.hybrid_account_alloc)}
  }
%{else~}
    "alloc": {
      ${join(",", formatlist("\"%s\" : { \"balance\": \"%s\" }", quorum_bootstrap_keystore.accountkeys-generator[*].account[0].address, quorum_bootstrap_keystore.accountkeys-generator[*].account[0].balance))}
    }
%{endif~}
}
EOF
}

resource "local_file" "nodekey-file" {
  filename = format("%s/nodekeys-tmp.json", quorum_bootstrap_network.this.network_dir_abs)
  content  = <<-EOF
    {
    ${join(",", quorum_bootstrap_node_key.nodekeys-generator[*].hex_node_id)}
}
EOF
}

resource "quorum_bootstrap_data_dir" "datadirs-generator" {
  count    = local.number_of_nodes
  data_dir = format("%s/%s%s", quorum_bootstrap_network.this.network_dir_abs, local.node_dir_prefix, count.index)
  genesis  = local_file.genesis-file[count.index].content
}

resource "local_file" "static-nodes" {
  count    = local.number_of_nodes
  filename = format("%s/static-nodes.json", quorum_bootstrap_data_dir.datadirs-generator[count.index].data_dir_abs)
  content  = "[${var.hybrid_network ? join(",", var.hybrid_enodeurls) : join(",", local.network.enode_urls)}]"
}

resource "local_file" "permissioned-nodes" {
  count    = local.number_of_nodes
  filename = format("%s/permissioned-nodes.json", quorum_bootstrap_data_dir.datadirs-generator[count.index].data_dir_abs)
  content  = local_file.static-nodes[count.index].content
}

resource "local_file" "passwords" {
  count    = local.number_of_nodes
  filename = format("%s/%s", quorum_bootstrap_data_dir.datadirs-generator[count.index].data_dir_abs, local.password_file)
  content  = ""
}

resource "local_file" "genesisfile" {
  count    = local.number_of_nodes
  filename = format("%s/%s", quorum_bootstrap_data_dir.datadirs-generator[count.index].data_dir_abs, local.genesis_file)
  content  = quorum_bootstrap_data_dir.datadirs-generator[count.index].genesis
}

resource "local_file" "tmconfigs-template-generator" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/config.tpl", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index)
  content = jsonencode(merge(jsondecode(<<-JSON
{
    "useWhiteList": false,
    "jdbc": {
        "username": "sa",
        "password": "",
        "url": "[TO BE OVERRIDREN FROM COMMAND LINE]",
        "autoCreateTables": true
    },
    "serverConfigs":[
        {
            "app":"ThirdParty",
            "enabled": true,
            "serverAddress": "http://${var.tm_networking[count.index].ip.private}:${var.tm_networking[count.index].port.thirdparty.internal}",
            "communicationType" : "REST"
        },
        {
            "app":"Q2T",
            "enabled": true,
            "serverAddress":"[TO BE OVERRIDDEN FROM COMMAND LINE]",
            "communicationType" : "REST"
        },
        {
            "app":"P2P",
            "enabled": true,
            "serverAddress":"http://${var.tm_networking[count.index].ip.private}:${var.tm_networking[count.index].port.p2p}",
            "sslConfig": {
              "tls": "OFF",
              "generateKeyStoreIfNotExisted": true,
              "serverKeyStore": "[TO BE OVERRIDREN FROM COMMAND LINE]",
              "serverKeyStorePassword": "quorum",
              "serverTrustStore": "[TO BE OVERRIDREN FROM COMMAND LINE]",
              "serverTrustStorePassword": "quorum",
              "serverTrustMode": "TOFU",
              "knownClientsFile": "[TO BE OVERRIDREN FROM COMMAND LINE]",
              "clientKeyStore": "[TO BE OVERRIDREN FROM COMMAND LINE]",
              "clientKeyStorePassword": "quorum",
              "clientTrustStore": "[TO BE OVERRIDREN FROM COMMAND LINE]",
              "clientTrustStorePassword": "quorum",
              "clientTrustMode": "TOFU",
              "knownServersFile": "[TO BE OVERRIDREN FROM COMMAND LINE]"
            },
            "communicationType" : "REST"
        }
    ],
    "peer": [${join(",", formatlist("{\"url\" : \"http://%s:%d\"}", var.tm_networking[*].ip.private, var.tm_networking[*].port.p2p))}],
    "keys": {
      "passwords": [],
      "keyData": [${var.hybrid_network? var.hybrid_tmkeys[count.index] : data.null_data_source.meta[count.index].inputs.tmKeys}]
    },
    "features" : {
%{if local.vnodes[count.index].mpsEnabled}
      "enableMultiplePrivateStates" : "true",
%{endif~}
%{if var.privacy_enhancements.enabled~}
      "enablePrivacyEnhancements" : "true",
%{endif~}
      "enableRemoteKeyValidation" : "true"
    },
%{if local.vnodes[count.index].mpsEnabled}
    "residentGroups":[
        %{for name, node in local.vnodes[count.index].vnodes~}
          {
              "name": "${node.name}",
              "description":"${node.name}",
              "members":[
                ${join(",", formatlist("\"%s\"", [for idx, keyName in node.tmKeys : element(local.public_key_b64, index(local.tm_named_keys_all, keyName))]))}
              ]
          },
        %{endfor~}
          {
              "name": "test",
              "description":"test",
              "members":[]
          }
    ],
%{endif~}
    "alwaysSendTo": []
}
JSON
  ), lookup(var.additional_tessera_config, count.index, {})))
}

// resolving any variable in the config
resource "local_file" "tmconfigs-generator" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/config.json", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index)
  content = templatefile(
    local_file.tmconfigs-template-generator[count.index].filename,
    merge(
      // named key variable to the actual value
      { for k in local.tm_named_keys_alloc[count.index] : k => element(local.public_key_b64, index(local.tm_named_keys_all, k)) },
      // additional variable defines here
      {},
    )
  )
}
