terraform {
  required_providers {
    quorum = {
      source = "ConsenSys/quorum"
      version = "0.2.0"
    }
  }
}

locals {
  network_name              = coalesce(var.network_name, random_string.network-name.result)
  generated_dir             = var.output_dir
  tmkeys_generated_dir      = "${local.generated_dir}/${local.network_name}/tmkeys"
  accountkeys_generated_dir = "${local.generated_dir}/${local.network_name}/accountkeys"
  node_dir_prefix           = "node-"
  tm_dir_prefix             = "tm-"
  ethsigner_dir_prefix      = "ethsigner-"
  keystore_folder             = "keystore"
  keystore_password_file             = "keystore_password"
  keystore_password          = "besito1"
  genesis_file              = "genesis.json"
  number_of_nodes           = max(length(var.besu_networking), length(var.tm_networking), length(var.ethsigner_networking))
  node_indices              = range(local.number_of_nodes)
  // 0-based node index
  // by default we allocate one named key per TM: K0, K1 ... Kn
  // this can be overrriden by the variable
  tm_named_keys_alloc = { for id in local.node_indices : id => [
  format("UnnamedKey%d", id)] }
  // by default we allocate one named account per node
  // this can be overrriden by the variable
  named_accounts_alloc = { for id in local.node_indices : id => [
  "Default"] }
  tm_named_keys_all        = flatten(values(local.tm_named_keys_alloc))
  node_initial_paticipants = { for id in local.node_indices : id => "true" }
  istanbul_validators      = { for id in local.node_indices : id => "true" }
}

resource "random_string" "network-name" {
  length    = 8
  number    = false
  special   = false
  min_lower = 8
}

# metadata containing information required to interact with the newly created network
resource "local_file" "configuration" {
  filename = format("%s/application-%s.yml", quorum_bootstrap_network.this.network_dir_abs, local.network_name)
  content  = <<-EOF
quorum:
  nodes:
%{for i in data.null_data_source.meta[*].inputs.idx~}
    ${format("Node%d:", i + 1)}
      istanbul-validator-id: ${quorum_bootstrap_node_key.nodekeys-generator[i].istanbul_address}
      enode-url: ${local.enode_urls[i]}
      account-aliases:
%{for idx, k in local.named_accounts_alloc[i]~}
        ${k}: "${element(quorum_bootstrap_keystore.accountkeys-generator[i].account.*.address, idx)}"
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
