# QBFT consensus - Less empty blocks

  Tags: qbft-transition-empty-block-period

This specification describes the behavior of empty block period
 - Starts with default parameters
 - Transition empty block period and block period

## Empty Block Period: less empty blocks are produced

  Tags: empty-block-period

* From block "1" to "109", produced empty blocks should have block periods to be at least "1"
* From block "120" to "179", produced empty blocks should have block periods to be at least "19"
* From block "180" to "190", produced empty blocks should have block periods to be at least "1"
