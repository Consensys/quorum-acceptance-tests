module.exports = function(config, wobj, log) {
    this.Web3 = wobj
    this.logger = log
    this.cfg = config
    const assert = require('assert')

    logger.info("smart contract lib initialized")

    this.checkTxnReceiptInNode = async function (nodeName, transactionHashes) {

        logger.debug("check txn receipts in nodeName:" + nodeName)
        //connect to the node we're checking
        var web3 = new this.Web3(new this.Web3.providers.HttpProvider(nodeName))

        //found is number of receipts found, missing is number missing, logs are the actual logs from found txns
        var outputObject = {found: 0, missing: 0, logs: []};

        for(var c=0; c < transactionHashes.length; ++c){
            var hash = transactionHashes[c]
            logger.debug((c+1) + ".check receipt for hash:" + hash)
            var receipt = await web3.eth.getTransactionReceipt(hash)
            if(receipt == null ){
                logger.error("receipt is null for hash:" + hash + " in node " + nodeName)
            }
            if (receipt != null && Array.isArray(receipt.logs) && receipt.logs.length > 0) {
                outputObject.found++;
                var removedZeroes = receipt.logs[0].data.replace(/0x0*/, '') || '0';
                outputObject.logs.push(removedZeroes)
            } else {
                outputObject.missing++;
            }
        }

        logger.debug("==============")
        return outputObject;
    };

    this.deployContractAndCheckReceiptsForEventLog = async function (privateFlag, nodeId, eventCnt, toNodeId) {
        var web3 = new Web3(new Web3.providers.HttpProvider(cfg.nodes()[nodeId]))

//sender address
        var sender = cfg.accounts()[nodeId];

//which event to fire
        const eventMethodSignature = 'emitEvent()';

//create the contract
        const TestContract = new web3.eth.Contract([{
            "constant": false,
            "inputs": [],
            "name": "emitEvent",
            "outputs": [],
            "payable": false,
            "stateMutability": "nonpayable",
            "type": "function"
        }, {
            "anonymous": false,
            "inputs": [{"indexed": false, "name": "count", "type": "uint256"}],
            "name": "TestEvent",
            "type": "event"
        }])
        var isPrivate = privateFlag;
        isPrivate ? logger.info("private contract") : logger.info("public contract")

        logger.info('Creating a contract on node', cfg.nodes()[nodeId])

        var options = {from: sender, gas: '4700000'};
        if (isPrivate) {
            options.privateFor = [cfg.constellations()[toNodeId]];
        }

        await TestContract.deploy({
            data: '0x608060405260008055348015601357600080fd5b5060cc806100226000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680637b0cb839146044575b600080fd5b348015604f57600080fd5b5060566058565b005b7f1440c4dd67b4344ea1905ec0318995133b550f168b4ee959a0da6b503d7d2414600080815480929190600101919050556040518082815260200191505060405180910390a15600a165627a7a72305820d0a22dbf268334eae6006b4d59e1f355fb738aa82345685173a2cc90fc6f085c0029'
        }).send(
            options
        ).on('receipt', function (receipt) {
            logger.debug('created contract address: ', receipt.contractAddress)
        }).then(async function (contractInstance) {

            var hashes = [];

            var optionsEvent = {from: sender, gas: '4700000'};

            if (isPrivate) {
                optionsEvent.privateFor = [cfg.constellations()[toNodeId]];
            }

            const txCnt = eventCnt
            logger.debug("nodeNo:" + nodeId + " eventCnt:"+ txCnt + (isPrivate ? " toNodeId:"+toNodeId : ""))
            for (var counter = 0; counter < txCnt; counter++) {

                var current = await contractInstance.methods[eventMethodSignature]().send(
                    optionsEvent
                )
                logger.debug("txn hash:" + current.transactionHash + " obj:" + JSON.stringify(current))
                hashes.push(current.transactionHash)
                logger.info("hashes len " + hashes.length + " top hash->" + hashes[hashes.length-1])

            }
            logger.debug("finished calling " +eventMethodSignature + " " + txCnt + " times" )


            for(var k=1; k <=cfg.nodesToTest(); ++k) {
                var node = cfg.nodes()[k]
                const results = await this.checkTxnReceiptInNode(node, hashes)
                logger.info('In node', node, ',', results.found, ' transactions were found and ', results.missing, ' were missing')
                logger.info('This node can see the following logs:', JSON.stringify(results.logs))
                if(isPrivate){
                    if(k == nodeId || k == toNodeId){
                        assert.equal(results.found, txCnt, "private contract txn - less no of transactions found in node " + node)
                    }else {
                        assert.equal(results.found, 0, "private contract txn - should not be visible to node " + node)
                    }
                }else{
                    assert.equal(results.found, txCnt, "public contract txn - less no of transactions found in node " + node)
                }
            }

        });
        logger.debug("********")
        return true

    }

    return this;
}