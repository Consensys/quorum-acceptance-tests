#
#  This network setups with 4 nodes
#

number_of_nodes = 4
consensus       = "istanbul"
# Import images so they can be used programatically in the test
docker_images = ["quorumengineering/quorum:2.5.0", "quorumengineering/tessera:0.10.5", "quorumengineering/quorum:21.10.0", "quorumengineering/tessera:21.10.0", "quorumengineering/quorum:develop", "quorumengineering/tessera:develop"]

privacy_enhancements = { block = 0, enabled = false }
