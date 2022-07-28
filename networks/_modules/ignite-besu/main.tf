terraform {
  required_providers {
    quorum = {
      source  = "ConsenSys/quorum"
      version = "0.3.0"
    }
  }
  experiments = [ module_variable_optional_attrs ]
}

locals {
  network_name              = coalesce(var.network_name, random_string.network-name.result)
  generated_dir             = var.output_dir
  tmkeys_generated_dir      = "${local.generated_dir}/${local.network_name}/tmkeys"
  accountkeys_generated_dir = "${local.generated_dir}/${local.network_name}/accountkeys"
  node_dir_prefix           = "besu-"
  tm_dir_prefix             = "tm-"
  ethsigner_dir_prefix      = "ethsigner-"
  keystore_folder           = "keystore"
  keystore_password_file    = "keystore_password"
  keystore_password         = "besito1"
  genesis_file              = "genesis.json"
  number_of_nodes           = max(length(var.besu_networking), length(var.ethsigner_networking))
  number_of_quorum_nodes    = var.number_of_quorum_nodes
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
  tm_named_keys_all = flatten(values(local.tm_named_keys_alloc))
  node_initial_paticipants = merge(
    { for id in local.node_indices : id => "true" }, // default to true for all
    { for id in var.exclude_initial_nodes : id => "false" }
  )
  istanbul_validators = { for id in local.node_indices : id => "true" }
  hybrid_network      = var.hybrid_network

  key_data       = var.hybrid_network ? var.hybrid_key_data : quorum_transaction_manager_keypair.tm.*.key_data
  public_key_b64 = var.hybrid_network ? var.hybrid_public_key_b64 : quorum_transaction_manager_keypair.tm.*.public_key_b64

  total_node_indices = var.hybrid_network ? [for id in range(local.number_of_nodes) : sum([id, local.number_of_quorum_nodes])] : range(local.number_of_nodes)
}

resource "random_string" "network-name" {
  length    = 8
  number    = false
  special   = false
  min_lower = 8
}

# metadata containing information required to interact with the newly created network
resource "local_file" "configuration" {
  count    = local.hybrid_network ? 0 : 1
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
        ${k}: ${element(local.public_key_b64, index(local.tm_named_keys_all, k))}
%{endfor~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}
      third-party-url: ${data.null_data_source.meta[i].inputs.tmThirdpartyUrl}
      graphql-url: ${data.null_data_source.meta[i].inputs.graphqlUrl}
%{endfor~}
EOF
}
