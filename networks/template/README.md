A typical 4-node network serving as a template network. It means that containers are created but not started.
It also provides Docker endpoint which can be used to manage the network.

Created containers are served as templates which can be used to create other containers by copying volume bindings,
network configuration, endpoints, environments ...

## Environment Variables

* `ADDITIONAL_GETH_ARGS`: string value being appended to the existing `geth` arguments