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
  node_dir_prefix           = "quorum-"
  tm_dir_prefix             = "tm-"
  password_file             = "password.txt"
  latest_genesis_file       = "latest-genesis.json"
  legacy_genesis_file       = "legacy-genesis.json"
  number_of_nodes           = length(var.geth_networking)
  node_indices              = range(local.number_of_nodes) // 0-based node index
  qlight_client_indices = [for k in keys(var.qlight_clients) : parseint(k, 10)] # map keys are string type, so convert to int
  non_qlight_client_node_indices    = [for idx in local.node_indices : idx if !contains(local.qlight_client_indices, idx)] // nodes in the consensus (e.g. not a qlight client node)
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
    { for id in var.non_validator_nodes : id => "false" },
    { for id in local.qlight_client_indices : id => "false" }
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
%{if var.consensus == "istanbul" || var.consensus == "qbft"~}
      istanbul-validator-id: "${quorum_bootstrap_node_key.nodekeys-generator[i].istanbul_address}"
%{endif~}
%{if local.vnodes[i].mpsEnabled~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}/?PSI=${b.name}
%{endif~}
%{if !local.vnodes[i].mpsEnabled~}
      url: ${data.null_data_source.meta[i].inputs.nodeUrl}
%{endif~}
      enode-url: ${local.enode_urls[i]}
%{if lookup(var.qlight_clients, i, null) != null~}
      qlight:
        is-client: true
        server-id: ${format("Node%s", var.qlight_clients[i].server_idx + 1)}
        psi: ${var.qlight_clients[i].mps_psi}
      third-party-url: ${data.null_data_source.meta[var.qlight_clients[i].server_idx].inputs.tmThirdpartyUrl}
%{else~}
      qlight:
        is-client: false
      third-party-url: ${data.null_data_source.meta[i].inputs.tmThirdpartyUrl}
%{endif~}
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
%{if lookup(var.qlight_clients, i, null) != null~}
%{if false~}
#       (This is a comment for the below terraform logic and should not be included in the file contents)
#       If the node is a qlight client, its available accounts should be those of its qlight server, e.g. see
#       terraform.qbft-qlight.tfvars (the qlight client vnode 'Node5' is configured with ethKeys '[EthKey0]').
#       What we are doing below is getting the value of 'EthKey0' (for example) from the generated accountkeys of the
#       qlight client's corresponding qlight server node.
#
%{endif~}
        ${name}: "${element(
            quorum_bootstrap_keystore.accountkeys-generator[index(local.non_qlight_client_node_indices, var.qlight_clients[i].server_idx)].account.*.address,
            index(local.named_accounts_alloc[var.qlight_clients[i].server_idx], name)
        )}"
%{else~}
%{if false~}
#       (This is a comment for the below terraform logic and should not be included in the file contents)
#       If the node is not a qlight client (i.e. a qlight server or a normal quorum node), we can simply use the
#       generated accountkeys for that node.
#
%{endif~}
        ${name}: "${element(
            quorum_bootstrap_keystore.accountkeys-generator[index(local.non_qlight_client_node_indices, parseint(i, 10))].account.*.address,
            index(local.named_accounts_alloc[i], name)
        )}"
%{endif~}
%{endfor~}
%{endfor~}
%{endfor~}
EOF
}
