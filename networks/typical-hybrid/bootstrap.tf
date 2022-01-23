locals {
  besu_addr   = [for idx in local.besu_node_indices : quorum_bootstrap_node_key.besu-nodekeys-generator[idx].istanbul_address if lookup(local.istanbul_validators, idx + local.number_of_quorum_nodes, "false") == "true"]
  quorum_addr = [for idx in local.quorum_node_indices : quorum_bootstrap_node_key.quorum-nodekeys-generator[idx].istanbul_address if lookup(local.istanbul_validators, idx, "false") == "true"]

  besu_alloc   = formatlist("\"%s\" : { \"balance\": \"%s\" }", quorum_bootstrap_keystore.besu-accountkeys-generator[*].account[0].address, quorum_bootstrap_keystore.besu-accountkeys-generator[*].account[0].balance)
  quorum_alloc = formatlist("\"%s\" : { \"balance\": \"%s\" }", quorum_bootstrap_keystore.quorum-accountkeys-generator[*].account[0].address, quorum_bootstrap_keystore.quorum-accountkeys-generator[*].account[0].balance)
}

resource "quorum_bootstrap_node_key" "besu-nodekeys-generator" {
  count = local.number_of_besu_nodes
}

resource "quorum_bootstrap_node_key" "quorum-nodekeys-generator" {
  count = local.number_of_quorum_nodes
}

resource "quorum_bootstrap_istanbul_extradata" "hybrid" {
  istanbul_addresses = concat(local.quorum_addr, local.besu_addr)
  mode               = "qbft"
}

locals {
  besu_enode_urls = formatlist("\"enode://%s@%s:%d\"", quorum_bootstrap_node_key.besu-nodekeys-generator[*].hex_node_id, local.besu_networking[*].ip.private, local.besu_networking[*].port.p2p)

  # metadata for network subjected to initial participants input
  besu_network = {
    hexNodeIds = [for idx in local.besu_node_indices : quorum_bootstrap_node_key.besu-nodekeys-generator[idx].hex_node_id if lookup(local.besu_node_initial_paticipants, idx, "false") == "true"]
    networking = [for idx in local.besu_node_indices : local.besu_networking[idx] if lookup(local.besu_node_initial_paticipants, idx, "false") == "true"]
    enode_urls = [for idx in local.besu_node_indices : local.besu_enode_urls[idx] if lookup(local.besu_node_initial_paticipants, idx, "false") == "true"]
  }
}

locals {
  quorum_enode_urls = formatlist("\"enode://%s@%s:%d\"", quorum_bootstrap_node_key.quorum-nodekeys-generator[*].hex_node_id, local.geth_networking[*].ip.private, local.geth_networking[*].port.p2p)

  # metadata for network subjected to initial participants input
  quorum_network = {
    hexNodeIds      = [for idx in local.quorum_node_indices : quorum_bootstrap_node_key.quorum-nodekeys-generator[idx].hex_node_id if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
    geth_networking = [for idx in local.quorum_node_indices : local.geth_networking[idx] if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
    enode_urls      = [for idx in local.quorum_node_indices : local.quorum_enode_urls[idx] if lookup(local.quorum_initial_paticipants, idx, "false") == "true"]
  }
}

locals {
  enode_urls = concat(local.quorum_network.enode_urls, local.besu_network.enode_urls)
}

resource "random_integer" "hybrid_network_id" {
  max = 3000
  min = 1400
}

resource "quorum_bootstrap_network" "this" {
  name       = local.network_name
  target_dir = local.generated_dir
}

resource "quorum_bootstrap_keystore" "besu-accountkeys-generator" {
  count                = local.number_of_besu_nodes
  keystore_dir         = format("%s/%s", local.ethsigner_dirs[count.index], local.keystore_folder)
  use_light_weight_kdf = true

  dynamic "account" {
    for_each = lookup(local.named_accounts_alloc, sum([count.index, local.number_of_quorum_nodes]))
    content {
      passphrase = local.keystore_password
      balance    = "1000000000000000000000000000"
    }
  }
  provisioner "local-exec" {
    command = "chmod 644 ${format("%s/%s", local.ethsigner_dirs[count.index], local.keystore_folder)}/*"
  }
}

resource "quorum_bootstrap_keystore" "quorum-accountkeys-generator" {
  count                = local.number_of_quorum_nodes
  keystore_dir         = format("%s/%s%s/keystore", quorum_bootstrap_network.this.network_dir_abs, local.quorum_node_dir_prefix, count.index)
  use_light_weight_kdf = true

  dynamic "account" {
    for_each = lookup(local.named_accounts_alloc, count.index)
    content {
      passphrase = ""
      balance    = "1000000000000000000000000000"
    }
  }
}

resource "quorum_transaction_manager_keypair" "tm" {
  count = length(local.tm_named_keys_all)
  config {
    memory = 100000
  }
}

data "null_data_source" "meta" {
  count = local.number_of_quorum_nodes + local.number_of_besu_nodes
  inputs = {
    idx             = count.index
    tmKeys          = join(",", [for k in local.tm_named_keys_alloc[count.index] : element(quorum_transaction_manager_keypair.tm.*.key_data, index(local.tm_named_keys_all, k))])
    tmThirdpartyUrl = format("http://%s:%d", local.tm_networking[count.index].ip.public, local.tm_networking[count.index].port.thirdparty.external)
    nodeUrl         = count.index < local.number_of_quorum_nodes ? format("http://%s:%d", local.geth_networking[count.index].ip.public, local.geth_networking[count.index].port.http.external) : format("http://%s:%d", local.ethsigner_networking[count.index - local.number_of_quorum_nodes].ip.public, local.ethsigner_networking[count.index - local.number_of_quorum_nodes].port.external)
    graphqlUrl      = count.index < local.number_of_quorum_nodes ? (local.geth_networking[count.index].graphql ? format("http://%s:%d/graphql", local.geth_networking[count.index].ip.public, local.geth_networking[count.index].port.http.external) : "") : (format("http://%s:%d/graphql", local.besu_networking[count.index - local.number_of_quorum_nodes].ip.public, local.besu_networking[count.index - local.number_of_quorum_nodes].port.graphql.external))
  }
}

# metadata containing information required to interact with the newly created network
resource "local_file" "configuration" {
  filename = format("%s/application-%s.yml", quorum_bootstrap_network.this.network_dir_abs, local.network_name)
  content  = <<-EOF
quorum:
  nodes:
%{for i in data.null_data_source.meta[*].inputs.idx~}
    ${format("Node%d:", i + 1)}
%{if i < local.number_of_quorum_nodes~}
      istanbul-validator-id: ${quorum_bootstrap_node_key.quorum-nodekeys-generator[i].istanbul_address}
%{else~}
      istanbul-validator-id: ${quorum_bootstrap_node_key.besu-nodekeys-generator[i - local.number_of_quorum_nodes].istanbul_address}
%{endif~}
      enode-url: ${local.enode_urls[i]}
      account-aliases:
%{for idx, k in local.named_accounts_alloc[i]~}
%{if i < local.number_of_quorum_nodes~}
        ${k}: "${element(quorum_bootstrap_keystore.quorum-accountkeys-generator[i].account.*.address, idx)}"
%{else~}
        ${k}: "${element(quorum_bootstrap_keystore.besu-accountkeys-generator[i - local.number_of_quorum_nodes].account.*.address, idx)}"
%{endif~}
%{endfor~}
      privacy-address-aliases:
%{for k in local.tm_named_keys_alloc[i]~}
        ${k}: ${element(quorum_transaction_manager_keypair.tm.*.public_key_b64, index(local.tm_named_keys_all, k))}
%{endfor~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}
      third-party-url: ${data.null_data_source.meta[i].inputs.tmThirdpartyUrl}
      graphql-url: ${data.null_data_source.meta[i].inputs.graphqlUrl}
%{endfor~}
EOF
}
