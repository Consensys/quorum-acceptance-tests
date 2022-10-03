# QBFT consensus - Block Reward

  Tags: qbft-transition-block-reward

This specification describes the behavior of block reward
 - Starts with default parameters
 - Transition through different modes of block reward

## Block Reward: verify that reward is given to beneficiaries

  Tags: block-reward

* From block "1" to "109", account "0x0638e1574728b6d862dd5d3a3e0942c3be47d996" should see increase of "0" per block
* From block "110" to "119", account "0x0638e1574728b6d862dd5d3a3e0942c3be47d996" should see increase of "20" per block
* From block "120" to "249", account "0x0638e1574728b6d862dd5d3a3e0942c3be47d996" should see increase of "10" per block
* From block "120" to "249", account "0x9186eb3d20cbd1f5f992a950d808c4495153abd5" should see increase of "10" per block
* From block "250" to "300", account "0x0638e1574728b6d862dd5d3a3e0942c3be47d996" should see increase of "0" per block
