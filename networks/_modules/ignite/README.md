A Terraform module that uses [`terraform-provider-quorum`](https://github.com/terraform-providers/terraform-provider-quorum) 
to bootstrap resources locally in order to run a [Quorum](https://github.com/ConsenSys/quorum) network from scratch.

The generated resources are organized in a folder (created from `network_name` input) under `output_dir` folder.

E.g.: Resources for a 3-node network with name `my-network` would be generated with the structure below:

```text
my-network/
  |- node-0/
  |- node-1/
  |- node-2/
  |- tm-0/
  |- tm-1/
  |- tm-2/
  |- tmkeys/
  |- application-my-network.yml
  |- genesis.json
```

- `node-x`: `datadir` for `geth` node `x`
- `tm-x`: working directory for Tessera `x`
- `tmkeys`: Tessera private keys
- `application-my-network.yml`: metadata information about the network including Ethereum accounts, endpoints and Tessera public keys
- `genesis.json`: Genesis file which is used to do `geth init` on invididual node
