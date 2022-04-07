terraform {
  required_providers {
    quorum = {
      source  = "ConsenSys/quorum"
      version = "0.3.0"
    }
  }
}

locals {
  network_name              = coalesce(var.network_name, random_string.network-name.result)
  generated_dir             = var.output_dir
  tmkeys_generated_dir      = "${local.generated_dir}/${local.network_name}/tmkeys"
  accountkeys_generated_dir = "${local.generated_dir}/${local.network_name}/accountkeys"
  node_dir_prefix           = "quorum-"
  tm_dir_prefix             = "tm-"
  password_file             = "password.txt"
  latest_genesis_file       = "latest-genesis.json"
  legacy_genesis_file       = "legacy-genesis.json"
  number_of_nodes           = length(var.geth_networking)
  node_indices              = range(local.number_of_nodes) // 0-based node index
  hybrid_network            = var.hybrid_network
  // by default we allocate one named key per TM: K0, K1 ... Kn
  // this can be overrriden by the variable
  tm_named_keys_alloc = merge(
    { for id in local.node_indices : id => [format("UnnamedKey%d", id)] },
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
    { for id in var.exclude_initial_nodes : id => "false" },
    { for id in var.non_validator_nodes : id => "false" }
  )

  vnodes = merge(
    { for id in local.node_indices : id => {
      mpsEnabled = false,
      vnodes = {
        id = {
          name    = format("Node%s", id + 1)
          tmKeys  = local.tm_named_keys_alloc[id],
          ethKeys = local.named_accounts_alloc[id]
        }
      }
    } },
    var.override_vnodes
  )

  key_data       = var.hybrid_network ? var.hybrid_key_data : quorum_transaction_manager_keypair.tm.*.key_data
  public_key_b64 = var.hybrid_network ? var.hybrid_public_key_b64 : quorum_transaction_manager_keypair.tm.*.public_key_b64
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

%{for a, b in local.vnodes[i].vnodes~}
    ${format("%s:", b.name)}
%{if var.consensus == "istanbul"~}
      istanbul-validator-id: "${quorum_bootstrap_node_key.nodekeys-generator[i].istanbul_address}"
%{endif~}
%{if local.vnodes[i].mpsEnabled~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}/?PSI=${b.name}
%{endif~}
%{if !local.vnodes[i].mpsEnabled~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}
%{endif~}
      enode-url: ${local.enode_urls[i]}
      third-party-url: ${data.null_data_source.meta[i].inputs.tmThirdpartyUrl}
%{if local.vnodes[i].mpsEnabled~}
      graphql-url: ${data.null_data_source.meta[i].inputs.graphqlUrl}/?PSI=${b.name}
%{endif~}
%{if !local.vnodes[i].mpsEnabled~}
      graphql-url: ${data.null_data_source.meta[i].inputs.graphqlUrl}
%{endif~}
      privacy-address-aliases:
%{for k in b.tmKeys~}
        ${k}: ${element(local.public_key_b64, index(local.tm_named_keys_all, k))}
%{endfor~}
      account-aliases:
%{for k, name in b.ethKeys~}
        ${name}: "${element(quorum_bootstrap_keystore.accountkeys-generator[i].account.*.address, index(local.named_accounts_alloc[i], name))}"
%{endfor~}
%{endfor~}
%{endfor~}
EOF
}
