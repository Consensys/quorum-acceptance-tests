# QBFT consensus - Less empty blocks

  Tags: qbft-transition-empty-block-period

This specification describes the behavior of empty block period
 - Starts with default parameters
 - Transition empty block period and block period

## Empty Block Period: less empty blocks are produced

  Tags: empty-block-period

* From block "0" to "50", produced empty blocks should have block periods to be at least "1"
* From block "50" to "60", produced empty blocks should have block periods to be at least "5"
* From block "60" to "70", produced empty blocks should have block periods to be at least "1"
