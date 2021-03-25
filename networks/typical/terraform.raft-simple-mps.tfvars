# This network is to enable MPS without changing how client interacts with the network
# This represents a typical network upgrade scenario where a node (Node0) is setup to
# enable MPS

consensus = "raft"

# this is config will be merged to the default genesis "config" section per node
additional_genesis_config = {
  0 = {
    isMPS = true
  }
}