#
#  This network setups with 4 nodes
#

number_of_nodes = 4
consensus       = "istanbul"
# Import images so they can be used programatically in the test
docker_images = ["quorumengineering/quorum:2.5.0", "quorumengineering/tessera:0.10.5"]

privacy_enhancements = { block = 0, enabled = false }
