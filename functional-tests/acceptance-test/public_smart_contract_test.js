const assert = require('assert')
const Web3 = require('web3')
const cfg = require("./config")
const logger = require('tracer').console({level:cfg.logLevel()})

/*
This test case covers the following scenarios:
sending private smart contract transaction from one node to the other nodes and emitting events.
The emitted events are checked and expected count of events in the all nodes are asserted
NOTE:
We should test with high txn count for ISTANBUL to simulate events getting distributed in multiple blocks
 */



// Checks a node for receipts given a set of transaction hashes
async function checkTxnReceiptInNode(nodeName, transactionHashes) {
    //connect to the node we're checking
    const web3 = new Web3(new Web3.providers.HttpProvider(nodeName))

    //found is number of receipts found, missing is number missing, logs are the actual logs from found txns
    const outputObject = {found: 0, missing: 0, logs: []};

    const promises = transactionHashes.map((hash) => {
        return web3.eth.getTransactionReceipt(hash)
})
    logger.debug("get receipts for transactions in " + nodeName + " started...")
    const receipts = await Promise.all(promises)
    logger.debug("get receipts for transactions in " + nodeName + " ended")

    receipts.forEach((receipt) => {
        if (Array.isArray(receipt.logs) && receipt.logs.length > 0) {
        outputObject.found++;
        let removedZeroes = receipt.logs[0].data.replace(/0x0*/, '') || '0';
        outputObject.logs.push(removedZeroes)
    } else {
        outputObject.missing++;
    }
})
    return outputObject;
}

async function testContract(privateFlag, nodeId) {
    let web3 = new Web3(new Web3.providers.HttpProvider(cfg.nodes()[nodeId]))

//sender address
    const sender = cfg.accounts()[nodeId];

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
    let isPrivate = privateFlag;
    isPrivate ? logger.info("Sending private transactions") : logger.info("Sending public transactions")

    logger.info('Creating a contract on node', cfg.nodes()[nodeId])

    let options = {from: sender, gas: '4700000'};
    if (isPrivate) {
        options.privateFor = ["ROAZBWtSacxXQrOe3FGAqJDyJjFePR5ce4TSIzmJ0Bc="];
    }

    await TestContract.deploy({
        data: '0x608060405260008055348015601357600080fd5b5060cc806100226000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680637b0cb839146044575b600080fd5b348015604f57600080fd5b5060566058565b005b7f1440c4dd67b4344ea1905ec0318995133b550f168b4ee959a0da6b503d7d2414600080815480929190600101919050556040518082815260200191505060405180910390a15600a165627a7a72305820d0a22dbf268334eae6006b4d59e1f355fb738aa82345685173a2cc90fc6f085c0029'
    }).send(
        options
    ).on('receipt', function (receipt) {
        logger.debug('Contract address: ', receipt.contractAddress)
    }).then(async function (contractInstance) {

        let hashes = [];
        let promises = [];

        let optionsEvent = {from: sender};
        if (isPrivate) {
            optionsEvent.privateFor = ["ROAZBWtSacxXQrOe3FGAqJDyJjFePR5ce4TSIzmJ0Bc="];
        }

        const txCnt = 10
        for (let counter = 0; counter < txCnt; counter++) {

            let current = await contractInstance.methods[eventMethodSignature]().send(
                optionsEvent
            ).on('receipt', function () {
            }).on('transactionHash', function (hash) {
                hashes.push(hash)
            })
            logger.info("hashes len " + hashes.length + " top hash->" + hashes[hashes.length-1])

        }
        logger.debug("end all promise")


        for(var k=1; k <=cfg.nodesToTest(); ++k) {
            var node = cfg.nodes()[k]
            const results = await checkTxnReceiptInNode(node, hashes)
            logger.info('In node', node, ',', results.found, 'transactions were found and', results.missing, 'were not')
            logger.info('This node can see the following logs:', JSON.stringify(results.logs))
            if(isPrivate){
                if(k == nodeId || k == 7){
                    assert.equal(results.found, txCnt, "private contract txn - less no of transactions found in node " + node)
                }else {
                    assert.equal(results.found, 0, "private contract txn - should not be visible to node " + node)
                }
            }else{
                assert.equal(results.found, txCnt, "public contract txn - less no of transactions found in node " + node)
            }
         }

    })

    return true

}

describe("public contract with emitEvent", function () {

    describe("sending from node1", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 1)
            assert.equal(res, true)
        })
    })

    describe("sending from node2", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 2)
            assert.equal(res, true)
        })
    })

    describe("sending from node3", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 3)
            assert.equal(res, true)
        })
    })

    describe("sending from node4", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 4)
            assert.equal(res, true)
        })
    })

    describe("sending from node5", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 5)
            assert.equal(res, true)
        })
    })

    describe("sending from node6", function (){
        it('should have same number of events/logs visible in all nodes', async function () {
            var res = await testContract(false, 6)
            assert.equal(res, true)
        })
    })

})
