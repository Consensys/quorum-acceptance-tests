#
#  This network setups with 4 nodes
#

number_of_nodes = 4
consensus       = "raft"
# Import images so they can be used programatically in the test
docker_images   = ["quorumengineering/quorum:2.5.0"]
addtional_geth_args = "--allow-insecure-unlock"
