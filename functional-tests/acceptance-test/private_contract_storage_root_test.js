const assert = require('assert')
const cfg = require("./config")
const util = require("./util")
const Web3 = require('web3')
const cp = require("child-process-promise")
const cp1 = require("child-process-promise")
const logger = require('tracer').console({level:cfg.logLevel()})
var storageRootArr = {}
const fromNodeId="1"
const toNodeId="7"

/*
This test case covers the following scenario:
The storage root should be same in all the nodes that participated in a private transaction and it should be different in other nodes.

TODO: storgaeRoot method should be added to Quorums web3-js and it should be used instead of testing via geth
*/



async function testStorageRoot(){

  const nodeName = cfg.nodes()[parseInt(fromNodeId)]
    var cmd = 'geth attach --exec \'loadScript(\"' + cfg. basePath() + 'private-contract.js\")\' ipc:' + cfg.qdataPath() + '/dd' + fromNodeId + '/geth.ipc'
    await cp.exec(cmd).then(async (result) => {
        var err = result.err
      var stdout = result.stdout
      logger.debug('err:' + result.err)
      logger.debug('stdout:' + result.stdout)
        if (err) {
            logger.info(err);
            logger.info("private contract creation failed")
            return false;
        }
        var msg = stdout
        if(msg.indexOf("Contract transaction send:") != -1){
          var pat = "TransactionHash:["
            var i0 = msg.indexOf(pat)
            var i1 = i0 + pat.length
            var i2 = msg.indexOf("]",i1)
            var transactionHash = msg.substring(i1,i2)


          await util.sleep (10000)
          var web3 = new Web3(new Web3.providers.HttpProvider(nodeName))
          var txnRcpt = await web3.eth.getTransactionReceipt(transactionHash)
          var contractAddr = txnRcpt.contractAddress


          await getStorageRootFromNode(fromNodeId, contractAddr)
          await getStorageRootFromNode("3",contractAddr )
          await getStorageRootFromNode(toNodeId, contractAddr)


            logger.debug("n1 storage Root="+storageRootArr[fromNodeId])
            logger.debug("n3 storage Root="+storageRootArr["3"])
            logger.debug("n7 storage Root="+storageRootArr[toNodeId])
        }
    }).catch(function (err) {
        console.error('ERROR: ', err);
    });

    logger.log("before r1")
    return true
}

async function getStorageRootFromNode(nodeId, contractAddress){
    var cmd = cfg.basePath()+"storage-root.sh " + nodeId +" " + contractAddress
    storageRootArr[nodeId] = null
    await cp1.exec(cmd).then(function (result) {
        var err = result.err
        var stdout = result.stdout
        if (err) {
            logger.error(err);
            logger.error("getting contract storage failed")
            return null;
        }
        var msg = stdout
        var e1 = msg.indexOf("Error: invalid address")
        var pat = "storageRoot:["
        var j1 = msg.indexOf(pat)
        var i0 = msg.indexOf(pat)
        var i1 = i0 + pat.length
        var i2 = msg.indexOf("]",i1)
        var storageRoot = msg.substring(i1,i2)
        logger.debug("storage root:->" + storageRoot)
        logger.debug("msg->" + msg)

        if(e1 != -1){
            storageRoot = null
            logger.error("ERROR: invalid address")
        } else if(j1 != -1){
            var i0 = msg.indexOf(pat)
            var i1 = i0 + pat.length
            var i2 = msg.indexOf("]",i1)
            var storageRoot = msg.substring(i1,i2)
            storageRootArr[nodeId] = storageRoot
            logger.debug((new Date()) + " amal storage root:->" + storageRoot)
        }

    }).catch(function (err) {
        console.error('ERROR: ', err);
    });
    logger.debug(new Date() + " before r2 -> " + storageRootArr[nodeId])
    return true
}






describe("storage root for private smart contract from node1 to node7", function () {

    it('should have same store root for a contract in node1 and node7 but different in other nodes', async () => {
    var res =  await testStorageRoot()
    assert.equal(res, true, "test failed")
    assert.notEqual(storageRootArr["1"], null, "storage root null in node1")
    assert.notEqual(storageRootArr["3"], null, "storage root null in node3")
    assert.notEqual(storageRootArr["7"], null, "storage root null in node7")
    assert.equal(storageRootArr["1"], storageRootArr["7"], "storage root is not same in node"+fromNodeId+" and node"+toNodeId+" for private smart contract")
    assert.notEqual(storageRootArr["1"], storageRootArr["3"], "storage root is same in node1 and node3 for private smart contract")
})
})

