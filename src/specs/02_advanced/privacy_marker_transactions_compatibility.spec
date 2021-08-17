# Privacy marker transactions in mixed network

  Tags: privacy-precompile-compatibility, networks/template::raft-4nodes, networks/template::istanbul-4nodes, pre-condition/no-record-blocknumber

  Quorum 21.7.1 introduces the privacy precompile contract and privacy marker transactions. Once the
  privacyPrecompileBlock is reached nodes can with `--privacymarker.enable` will create PMTs by default.
  Nodes without `--privacymarker.enable` should create normal private transactions by default but should still be able
  to process any PMTs they are party to.

## PMT-enabled node creates privacy marker transactions

  Tags: post-condition/datadir-cleanup, post-condition/network-cleanup

* Start the PMT network with:
    | node  | --privacymarker.enable |
    |-------|------------------------|
    | Node1 | true                   |
    | Node2 | true                   |
    | Node3 | false                  |
    | Node4 | false                  |

// send PMT
* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* Transaction Hash is returned for "contract17"
* Transaction Receipt is present in "Node1" for "contract17_transactionHash"
* Transaction Receipt is present in "Node2" for "contract17_transactionHash"
* Transaction Receipt is present in "Node3" for "contract17_transactionHash"
* Transaction Receipt is present in "Node4" for "contract17_transactionHash"

// verify PMT and private tx properties
* "contract17_transactionHash" retrieved from "Node1" is a privacy marker transaction
* "contract17_transactionHash" retrieved from "Node4" is a privacy marker transaction
* "contract17_transactionHash"'s private transaction can be retrieved from "Node1"
* "contract17_transactionHash"'s private transaction can be retrieved from "Node4"
* "contract17_transactionHash"'s private transaction cannot be retrieved from "Node2"
* "contract17_transactionHash"'s private transaction cannot be retrieved from "Node3"
* "contract17_transactionHash"'s PMT and private transaction retrieved from "Node1" share sender and nonce

// verify PMT updates state for all participants
* "contract17"'s `get()` function execution in "Node1" returns "42"
* "contract17"'s `get()` function execution in "Node4" returns "42"
* "contract17"'s `get()` function execution in "Node2" returns "0"
* "contract17"'s `get()` function execution in "Node3" returns "0"

// send normal private tx
* Execute "contract17"'s `set()` function with new value "5" in "Node4" and it's private for "Node1", store transaction hash as "contract17.set5"
* Transaction Receipt is present in "Node1" for "contract17.set5"
* Transaction Receipt is present in "Node2" for "contract17.set5"
* Transaction Receipt is present in "Node3" for "contract17.set5"
* Transaction Receipt is present in "Node4" for "contract17.set5"

// verify private tx properties
* "contract17.set5" retrieved from "Node1" is a normal private transaction, not a privacy marker transaction
* "contract17.set5" retrieved from "Node4" is a normal private transaction, not a privacy marker transaction

// verify private tx updates state for all participants
* "contract17"'s `get()` function execution in "Node1" returns "5"
* "contract17"'s `get()` function execution in "Node4" returns "5"
* "contract17"'s `get()` function execution in "Node2" returns "0"
* "contract17"'s `get()` function execution in "Node3" returns "0"
