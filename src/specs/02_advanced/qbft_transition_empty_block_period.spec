# QBFT consensus - Less empty blocks

  Tags: qbft-transition-empty-block-period

This specification describes the behavior of empty block period
 - Starts with default parameters
 - Transition empty block period and block period

## Empty Block Period: less empty blocks are produced

  Tags: empty-block-period

* From block "1" to "10", produced empty blocks should have block periods to be at least "1"
* From block "10" to "90", produced empty blocks should have block periods to be at least "10"
* From block "90" to "100", produced empty blocks should have block periods to be at least "1"
