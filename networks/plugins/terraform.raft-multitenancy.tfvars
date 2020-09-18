consensus    = "raft"
network_name = "raft-multitenancy"
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.0"
    expose_api = false
  }
}

number_of_nodes = 5

quorum_docker_image = { name = "local/quorum:multitenant", local = true }
tessera_docker_image = { name = "local/tessera:multitenant", local = true }

// TM key aliases
// start with one key pair per node
override_tm_named_key_allocation = {
  0 = ["A1"]
  1 = ["B1"]
  2 = ["C1"]
  3 = ["D1"]
  4 = ["E1"]
}