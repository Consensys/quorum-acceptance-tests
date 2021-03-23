locals {
  enode_urls = formatlist("\"enode://%s@%s:%d\"", quorum_bootstrap_node_key.nodekeys-generator[*].hex_node_id, var.besu_networking[*].ip.private)

  # metadata for network subjected to initial participants input
  network = {
    hexNodeIds      = [for idx in local.node_indices : quorum_bootstrap_node_key.nodekeys-generator[idx].hex_node_id if lookup(local.node_initial_paticipants, idx, "false") == "true"]
    networking = [for idx in local.node_indices : var.besu_networking[idx] if lookup(local.node_initial_paticipants, idx, "false") == "true"]
    enode_urls      = [for idx in local.node_indices : local.enode_urls[idx] if lookup(local.node_initial_paticipants, idx, "false") == "true"]
  }

  chainId = random_integer.network_id.result
}

data "null_data_source" "meta" {
  count = local.number_of_nodes
  inputs = {
    idx             = count.index
    tmKeys          = join(",", [for k in local.tm_named_keys_alloc[count.index] : element(quorum_transaction_manager_keypair.tm.*.key_data, index(local.tm_named_keys_all, k))])
    nodeUrl         = format("http://%s:%d", var.ethsigner_networking[count.index].ip.public, var.ethsigner_networking[count.index].port.http.external)
    tmThirdpartyUrl = format("http://%s:%d", var.tm_networking[count.index].ip.public, var.tm_networking[count.index].port.thirdparty.external)
    graphqlUrl      = format("http://%s:%d/graphql", var.besu_networking[count.index].ip.public, var.besu_networking[count.index].port.graphql.external)
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
  count                = local.number_of_nodes
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
  content  = quorum_transaction_manager_keypair.tm[count.index].key_data
}

data "quorum_bootstrap_genesis_mixhash" "this" {

}

resource "quorum_bootstrap_istanbul_extradata" "this" {
  istanbul_addresses = [for idx in local.node_indices : quorum_bootstrap_node_key.nodekeys-generator[idx].istanbul_address if lookup(local.istanbul_validators, idx, "false") == "true"]
}

resource "local_file" "genesis-file" {
  filename = format("%s/genesis.json", quorum_bootstrap_network.this.network_dir_abs)
  content  = <<-EOF
{
  "config" : {
    "chainId" : ${local.chainId},
    "constantinoplefixblock" : 0,
    "ibft2" : {
      "blockperiodseconds" : 1,
      "epochlength" : 30000,
      "requesttimeoutseconds" : 10
    },
    "isQuorum": true
  },
  "nonce" : "0x0",
  "timestamp": "0x00",
  "gasLimit" : "0xFFFFFF00",
  "difficulty" : "0x1",
  "mixhash": "${data.quorum_bootstrap_genesis_mixhash.this.istanbul}",
  "extraData": "${quorum_bootstrap_istanbul_extradata.this.extradata}",
  "coinbase" : "0x0000000000000000000000000000000000000000",
  "alloc": {
      ${join(",", formatlist("\"%s\" : { \"balance\": \"%s\" }", quorum_bootstrap_keystore.accountkeys-generator[*].account[0].address, quorum_bootstrap_keystore.accountkeys-generator[*].account[0].balance))}
    },
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

resource "local_file" "static-nodes" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/static-nodes.json", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index)
  content  = "[${join(",", local.network.enode_urls)}]"
}

resource "local_file" "permissioned-nodes" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/permissioned-nodes.json", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index)
  content  = local_file.static-nodes[count.index].content
}

resource "local_file" "passwords" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/%s", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index,local.password_file)
  content  = ""
}

resource "local_file" "genesisfile" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/%s", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index,local.genesis_file)
  content  = local_file.genesis-file.content
}

resource "local_file" "tmconfigs-generator" {
  count    = local.number_of_nodes
  filename = format("%s/%s%s/config.json", quorum_bootstrap_network.this.network_dir_abs, local.tm_dir_prefix, count.index)
  content  = <<-JSON
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
      "keyData": [${data.null_data_source.meta[count.index].inputs.tmKeys}]
    },
    "alwaysSendTo": [],
    "features" : {
      "enableRemoteKeyValidation" : "true"
    }
}
JSON
}
