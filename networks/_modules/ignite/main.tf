locals {
  network_name              = coalesce(var.network_name, random_string.network-name.result)
  generated_dir             = var.output_dir
  tmkeys_generated_dir      = "${local.generated_dir}/${local.network_name}/tmkeys"
  accountkeys_generated_dir = "${local.generated_dir}/${local.network_name}/accountkeys"
  node_dir_prefix           = "node-"
  tm_dir_prefix             = "tm-"
  password_file             = "password.txt"
  number_of_nodes           = max(length(var.geth_networking), length(var.tm_networking))
  node_indices              = range(local.number_of_nodes) // 0-based node index
  // by default we allocate one named key per TM: K0, K1 ... Kn
  // this can be overrriden by the variable
  tm_named_keys_alloc = merge(
    { for id in local.node_indices : id => [format("K%d", id)] },
    var.override_tm_named_key_allocation
  )
  // by default we allocate one named account per node
  // this can be overrriden by the variable
  named_accounts_alloc = merge(
    { for id in local.node_indices : id => ["Default"] },
    var.override_named_account_allocation
  )
  tm_named_keys_all = flatten(values(local.tm_named_keys_alloc))
  quorum_initial_paticipants = merge(
    { for id in local.node_indices : id => "true" }, // default to true for all
    { for id in var.exclude_initial_nodes : id => "false" }
  )
  istanbul_validators = merge(
    { for id in local.node_indices : id => "true" }, // default to true for all
    { for id in var.non_validator_nodes : id => "false" }
  )
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
%{ if var.concensus == "istanbul" ~}
      istanbul-validator-id: ${quorum_bootstrap_node_key.nodekeys-generator[i].istanbul_address}
%{ endif ~}
      enode-url: ${local.enode_urls[i]}
      account-aliases:
%{for idx, k in local.named_accounts_alloc[i]~}
        ${k}: ${element(quorum_bootstrap_keystore.accountkeys-generator[i].account.*.address, idx)}
%{endfor~}
      privacy-address-aliases:
%{for k in local.tm_named_keys_alloc[i]~}
        ${k}: ${element(quorum_transaction_manager_keypair.tm.*.public_key_b64, index(local.tm_named_keys_all, k))}
%{endfor~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}
      third-party-url: ${data.null_data_source.meta[i].inputs.tmThirdpartyUrl}
%{endfor~}
EOF
}