#
#  This network setups with 4 nodes
#

number_of_nodes = 4
consensus       = "istanbul"
# Import images so they can be used programatically in the test
docker_images   = ["quorumengineering/quorum:21.4.0", "quorumengineering/tessera:21.1.0"]

privacy_enhancements = { block = 0, enabled = false }
