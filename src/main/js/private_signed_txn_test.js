const EthereumTx = require('ethereumjs-tx')
const keythereum = require("keythereum")
const fs = require('fs')
const assert = require('assert')
const Web3 = require('web3')
const cfg = require("./config")
const util = require("./util1")
const logger = require('tracer').console({level:cfg.logLevel()})

/*
This test case covers the following scenarios:
sending signed private transaction from one node to the other nodes
NOTE:
you should pass nonce value in HEX format
chain id should match with netword id of the node (from raft/istanbul config)
 */

var NSize = cfg.nodesToTest()

async function testSignedTransactionFromNode(nid) {
    for (t = 1; t <= NSize; ++t) {
        if (nid != t)
            await sendSignedTransaction(nid, t)
    }
    return true
}

async function sendSignedTransaction(fromNodeId, toNodeId) {
    assert.notEqual(fromNodeId, toNodeId, "from node and to node should be different")
    logger.info("send private signed transaction from account in node" + fromNodeId + " to node" + toNodeId)
    const baseKeyPath = cfg.keysPath()
    const keyFile = baseKeyPath + "key" + fromNodeId
    const nodeName = cfg.nodes()[fromNodeId]
    const toAcct = cfg.accounts()[toNodeId]
    logger.info('using node', fromNodeId)
    //connect to block chain
    var web3 = new Web3(new Web3.providers.HttpProvider(nodeName))

    const keyJSON = fs.readFileSync(keyFile, 'utf8')
    const keyObj = JSON.parse(keyJSON)
    const password = ''
    const privateKey = keythereum.recover(password, keyObj)

    logger.info('privateKey of account from node: ', privateKey.toString('hex'))

    const fromAcctArr = await web3.eth.getAccounts()
    const fromAcct = fromAcctArr[0]
    logger.info('from account: ', fromAcct)


    var nonce = await web3.eth.getTransactionCount(fromAcct)

    logger.info('nonce: ', nonce)
    var nonceHex = '0x0' + nonce.toString(16).toUpperCase()
    logger.info("nonce hex:" + nonceHex)

    const constellationId = cfg.constellations()[toNodeId]

    let txParams = {
        privateFor: [ constellationId ],
        nonce: nonceHex,
        gasPrice: '0x00',
        gasLimit: '0x47b760',
        to: toAcct,
        value: '0x00',
        chainId: cfg.chainId(),
    }

    logger.info('tx payload: ', txParams)

    const tx = new EthereumTx(txParams)

    tx.sign(privateKey)
    var rawTx = '0x' + tx.serialize().toString('hex')
    logger.info('raw transaction: ', rawTx)
    var blockNumber = await web3.eth.getBlockNumber()
    var sentTx = await web3.eth.sendSignedTransaction(rawTx)
    logger.info("transactionHash:" + sentTx.transactionHash)
    var txnObj = await web3.eth.getTransaction(sentTx.transactionHash)
    logger.info("txn is successful, blockHash:"+ txnObj.blockHash)
    assert.notEqual(txnObj.blockHash, "", "invalid block hash txnHash:" + sentTx.transactionHash)
    assert.notEqual(txnObj.blockNumber, 0, "block number is not valid txnHash:" + sentTx.transactionHash)
    assert.notEqual(txnObj.blockNumber, blockNumber, "block number has not changed for txnHash:" + sentTx.transactionHash)
    return true
}


step('send signed private transaction from node <nodeNo> to all other nodes one by one', async(nodeNo) => {
    logger.debug("private sign transaction " + nodeNo)
    var res = await testSignedTransactionFromNode(nodeNo)
        assert.equal(res, true)
}
)