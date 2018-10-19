module.exports = function(config, wobj, log) {

    this.Web3 = wobj
    this.logger = log
    this.cfg = config
    const assert = require('assert')
    const EthereumTx = require('ethereumjs-tx')
    const keythereum = require("keythereum")
    const fs = require('fs')
    const util = require('./util1')

    this.sendSignedTransactionFromNode = async function(isPrivate, fromNodeId) {
        for (var toNodeId = 1; toNodeId <= cfg.nodesToTest(); ++toNodeId) {
            if (fromNodeId != toNodeId)
                await this.sendSignedTransaction(isPrivate, fromNodeId, toNodeId)
        }
        return true
    }

    this.sendSignedTransaction = async function(isPrivate, fromNodeId, toNodeId) {
        assert.notEqual(fromNodeId, toNodeId, "from node and to node should be different")
        const pvtStr = isPrivate ? "private" : "public"
        logger.info("send "+pvtStr + " signed transaction from account in node" + fromNodeId + " to node" + toNodeId)
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

        var nonceHex = '0x0' + nonce.toString(16).toUpperCase()
        var v = util.getRandomInt(500)
        var value = '0x0' + v.toString(16).toUpperCase()
        logger.info('nonce: ', nonce)
        logger.info('nonceHex: ', nonceHex)

        var txParams = {
            nonce: nonceHex,
            gasPrice: '0x00',
            gasLimit: '0x47b760',
            to: toAcct,
            value: value,
            chainId: cfg.chainId(),
        }

        if(isPrivate) {
            const constellationId = cfg.constellations()[toNodeId]
            txParams.privateFor = [constellationId]
        }

        logger.info('tx payload: ', txParams)

        const tx = new EthereumTx(txParams)

        tx.sign(privateKey)
        const serializedTx = tx.serialize()
        var rawTx = '0x' + serializedTx.toString('hex')
        logger.info('raw transaction: ', rawTx)
        var blockNumber = await web3.eth.getBlockNumber()
        var sentTx = await web3.eth.sendSignedTransaction(rawTx)
        logger.info("txnObj transactionHash:" + sentTx.transactionHash)
        var txnObj = await web3.eth.getTransaction(sentTx.transactionHash)
        logger.info("txn is successful, transactionHash:"+ txnObj.blockHash)
        assert.notEqual(txnObj.blockHash, "", "invalid block hash for txnHash:" + sentTx.transactionHash)
        assert.notEqual(txnObj.blockNumber, 0, "block number is not valid for txnHash:" + sentTx.transactionHash)
        assert.notEqual(txnObj.blockNumber, blockNumber, "block number has not changed for txnHash:" + sentTx.transactionHash)
        assert.equal(txnObj.from, fromAcct, "from account is not matching for txnHash:" + sentTx.transactionHash)
        logger.info("---------------------------------------------------------")
        return true
    }

    return this;
}