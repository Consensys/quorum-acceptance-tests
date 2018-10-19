
module.exports = {

    sleep: function (ms) {
        return new Promise(resolve => setTimeout(resolve, ms))
    },

    getRandomInt: function (max) {
        var x = Math.floor(Math.random() * Math.floor(max))
        while (x == 0) {
            x = Math.floor(Math.random() * Math.floor(max))
        }
        return x
    }/*,

    checkTxnReceiptInNode: async function (nodeName, transactionHashes) {
        const Web3 = require('web3')
        const cfg = require("./config")
        const logger = require('tracer').console({level:cfg.logLevel()})

        logger.debug("check txn receipts in nodeName:" + nodeName)
        //connect to the node we're checking
        var web3 = new Web3(new Web3.providers.HttpProvider(nodeName))

        //found is number of receipts found, missing is number missing, logs are the actual logs from found txns
        var outputObject = {found: 0, missing: 0, logs: []};

        for(var c=0; c < transactionHashes.length; ++c){
            var hash = transactionHashes[c]
            logger.debug((c+1) + ".AJ2-check receipt for hash:" + hash)
            var receipt = await web3.eth.getTransactionReceipt(hash)
            if(receipt == null ){
                logger.error("receipt is null for hash:" + hash)
            }
            if (receipt != null && Array.isArray(receipt.logs) && receipt.logs.length > 0) {
                outputObject.found++;
                var removedZeroes = receipt.logs[0].data.replace(/0x0*!/, '') || '0';
                outputObject.logs.push(removedZeroes)
            } else {
                outputObject.missing++;
            }
        }

        logger.debug("==============")
        return outputObject;
    }
*/

}