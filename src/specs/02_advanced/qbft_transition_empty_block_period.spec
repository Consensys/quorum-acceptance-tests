# QBFT consensus - Less empty blocks

  Tags: qbft-transition-empty-block-period

This specification describes the behavior of empty block period
 - Starts with default parameters
 - Transition empty block period and block period

## Empty Block Period: less empty blocks are produced

  Tags: empty-block-period

* From block "1" to "119", produced empty blocks should have block periods to be at least "1"
* From block "121" to "249", produced empty blocks should have block periods to be at least "2"
* From block "251" to "300", produced empty blocks should have block periods to be at least "1"
