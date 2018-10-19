/*
This test case covers the following scenarios:
sending signed public transaction from one node to the other nodes
NOTE:
you should pass nonce value in HEX format
chain id should match with netword id of the node (from raft/istanbul config)
*/

step('send signed transaction from node <nodeNo> to all other nodes one by one', async function(nodeNo){
    const assert = require('assert');
    const Web3 = require('web3');
    const cfg = require("config");
    const logger = require('tracer').console({level:cfg.logLevel()});
    const signTxn = require("lib/sign_txn")(cfg, Web3, logger);
    var res = await signTxn.sendSignedTransactionFromNode(false, nodeNo);
    assert.equal(res, true);
});
