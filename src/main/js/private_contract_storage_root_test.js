const assert = require('assert')
const cfg = require("./config")
const util = require("lib/util1")
const Web3 = require('web3')
const cp = require("child-process-promise")
const cp1 = require("child-process-promise")
const request = require('request');
const logger = require('tracer').console({level:cfg.logLevel()})

/*
This test case covers the following scenario:
The storage root should be same in all the nodes that participated in a private transaction and it should be different in other nodes.
*/



// wrap a request in an promise
function callRPC(reqData) {
    return new Promise((resolve, reject) => {
        request(reqData, (error, response, body) => {
        if (error) {
            logger.debug("failed error:" + error)
            reject(error);
        }
        if (response.statusCode != 200) {
            logger.error("failed " +response.statusCode )
            reject('Invalid status code <' + response.statusCode + '>');
        }
        //logger.debug("got response: body:" + JSON.stringify(body))
        //logger.debug("got response: response:" + JSON.stringify(response))
        resolve(body);
    });
});
}

async function getStorageRoot(fromNodeId, contractAddr) {
    var nodeName = cfg.nodes()[fromNodeId]
    var reqData = {
        url: nodeName,
        json: {"jsonrpc":"2.0", "method":"eth_storageRoot",
            "params":[ contractAddr ], "id":77} ,
        method: 'POST',
    };

    var storageRoot = "failed"
    try {
        logger.debug("called RPC for reqData:" + JSON.stringify(reqData) + " nodeName:"+ nodeName)
        var response = await callRPC(reqData)
        logger.debug("RPC call done")
        logger.debug("response obj:" + JSON.stringify(response))
        if(response.error){
            logger.error("respObj has error " + response.error)
        }else{
            storageRoot = response.result
        }
    }catch (e) {
        logger.error("error " + e + " -> fetching storage root for contract address:" + contractAddr + " from node " + fromNodeId)
    }

    return storageRoot

}


async function CheckStorageRoot(fromNodeId, toNodeId){
    logger.debug("from node id:" + fromNodeId + " to node id:" + toNodeId)
    var fromNodeName = cfg.nodes()[fromNodeId]
    var web3 = new Web3(new Web3.providers.HttpProvider(fromNodeName))
    var abi = [{"constant":true,"inputs":[],"name":"storedData","outputs":[{"name":"","type":"uint256"}],"payable":false,"type":"function"},{"constant":false,"inputs":[{"name":"x","type":"uint256"}],"name":"set","outputs":[],"payable":false,"type":"function"},{"constant":true,"inputs":[],"name":"get","outputs":[{"name":"retVal","type":"uint256"}],"payable":false,"type":"function"},{"inputs":[{"name":"initVal","type":"uint256"}],"payable":false,"type":"constructor"}];

    var bytecode = "0x6060604052341561000f57600080fd5b604051602080610149833981016040528080519060200190919050505b806000819055505b505b610104806100456000396000f30060606040526000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632a1afcd914605157806360fe47b11460775780636d4ce63c146097575b600080fd5b3415605b57600080fd5b606160bd565b6040518082815260200191505060405180910390f35b3415608157600080fd5b6095600480803590602001909190505060c3565b005b341560a157600080fd5b60a760ce565b6040518082815260200191505060405180910390f35b60005481565b806000819055505b50565b6000805490505b905600a165627a7a72305820d5851baab720bba574474de3d09dbeaabc674a15f4dd93b974908476542c23f00029";

    const TestContract = new web3.eth.Contract(abi)
    var isPrivate = true;
    isPrivate ? logger.info("private contract") : logger.info("public contract")

    logger.info('Creating a contract on node', fromNodeName)
    var sender = cfg.accounts()[fromNodeId];
    var options = {from: sender, gas: '4700000'};
    if (isPrivate) {
        options.privateFor = [cfg.constellations()[toNodeId]];
    }

    logger.debug("send options " + JSON.stringify(options))

    await TestContract.deploy({ data: bytecode, arguments:[42]}).send(
        options
    ).on('receipt', async function (receipt) {
        logger.debug("receipt:" + JSON.stringify(receipt))
    }).then(async function (contractInstance) {
        logger.debug("contractInstance created.")


        logger.debug('created contract address: ', contractInstance.options.address)
        var contractAddr = contractInstance.options.address
        var storageRootInFromNode = await getStorageRoot(fromNodeId, contractAddr)
        var storageRootInToNode = await getStorageRoot(toNodeId, contractAddr)
        logger.debug("storageRootInFromNode="+storageRootInFromNode)
        logger.debug("storageRootInToNode="+storageRootInToNode)
        assert.notEqual(storageRootInFromNode, "failed", "storage root is missing in node " + fromNodeId)
        assert.notEqual(storageRootInToNode, "failed ", "storage root is missing in node " + fromNodeId )
        assert.equal(storageRootInFromNode, storageRootInToNode, "The storage root is not same in node " + fromNodeId + " and " + toNodeId )
        for(var j=1; j <=7; ++j){
            if(j != fromNodeId && j != toNodeId){
                var storageRootInOtherNode = await getStorageRoot(j, contractAddr)
                logger.debug("storage root in node " + j + " is " + storageRootInOtherNode)
                assert.notEqual(storageRootInOtherNode, storageRootInFromNode, "Storage root in node " + j + " is matching with node " + fromNodeId + " and " + toNodeId)
            }
        }
    })
    return true
}


step('private contract created in node <fromNodeNo> with private for node <toNodeNo> should have same storage root in both nodes only', async function(fromNodeNo, toNodeNo) {
    var res = await CheckStorageRoot(fromNodeNo, toNodeNo)
    assert.equal(res, true)
})


