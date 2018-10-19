const assert = require('assert')
const Web3 = require('web3')
const logger = require('tracer').console({level:"warn"})

const cfg = require('config')
const pubTxn = require("lib/public_txn")(cfg, Web3, logger)

/*
This test case covers the following scenarios:
sending  public transaction from one node to the other nodes.
 */


step('send transaction from one node to other nodes - run transactions in parallel from all 7 nodes', async () => {
    var res = await pubTxn.sendPublicTransactionInParallel();
    assert.equal(res.length, cfg.nodesToTest(), "test failed in some nodes");
});


step('send transaction from node <nodeNo> to all other nodes one by one', async(nodeNo) => {
    var res = await pubTxn.sendPublicTransaction(nodeNo, 500);
    assert.equal(res, true);
});


