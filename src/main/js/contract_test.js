
//check if a transaction has logs populated by verifying the transaction receipt in all nodes
const assert = require('assert')
const Web3 = require('web3')
const logger = require('tracer').console({level:'debug'})

async function checkTxnHashForLogInAllNodes() {

    var nodesFound = [];

for(var j=0; j <7; ++j) {
    var hash = "0xd0a86902fd9dd412cc487355268d0853c7338ea74a9aea442fc784d3b7878bbc"

    console.log("node"+(j+1))
    var web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:2200"+j))

    var receipt = await
    web3.eth.getTransactionReceipt(hash)
    if (receipt == null) {
        logger.error("receipt is null for hash:" + hash + " in node " + nodeName)
    } else {
        if(receipt.logs.length == 0)
            console.log("event not found")
        else {
            console.log("event found")
            nodesFound[nodesFound.length] = j+1
            logger.info("receipt obj:" + JSON.stringify(receipt))
        }


    }
}

console.log("event found in nodes:" + nodesFound)

}



//checkTxnHashForLogInAllNodes()
