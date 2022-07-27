# Qlight client connecting to a multitenant qlight server node

 Tags: multitenancy-qlight-client, networks/plugins::qbft-qlight-multitenancy-alt, pre-condition/no-record-blocknumber

These tests are tailored for the configuration of networks/plugins::qbft-qlight-multitenancy-alt. 

node 1 ql client (server = node 5, psi JPM)
node 2 standard node
node 3 standard node
node 4 standard node
node 5 ql server + mt node (PSIs=[JPM, GS])

Node 1 is configured with the OAuth2 scope `psi://JPM?self.eoa=0x0` - so externally signed transactions can be sent but
unsigned transactions are not allowed.  This is enough to test the OAuth2 scopes/PSI are enforced correctly by the
qlight server.

* Configure `"JPM Investment"` in authorization server to access `"Node5"` with:
    | scope                    |
    |--------------------------|
    | `rpc://eth_*`            |
    | `psi://JPM?node.eoa=0x0&self.eoa=0x0` |

// see above for description of network config
## Qlight client can make requests to an MPS Qlight server if permitted by its configured Oauth2 scope

* Deploy a simple smart contract with initial value "52" signed by external wallet "Wallet1" in "Node1" and it's private for "Node2", name this contract as "contract52"
* Transaction Hash is returned for "contract52"
* Transaction Receipt is present in "Node1" for "contract52" from external wallet "Wallet1"
* Transaction Receipt is present in "Node2" for "contract52" from external wallet "Wallet1"
* `"JPM Investment"` reads the value from "contract52" successfully by sending a request to `"Node5"` and the value returns "52"

// see above for description of network config
## Qlight client cannot make requests to an MPS Qlight server if not permitted by its configured Oauth2 scope

* Deploying a "StandardPrivate" simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node2" fails with message "Error processing transaction request: not authorized"
