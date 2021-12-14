/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.EthChainId;
import com.quorum.gauge.sol.*;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.util.Collections;

@Service
public class PermissionsContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsContractService.class);
    @Autowired
    protected RPCService rpcService;

    @Autowired
    AccountService accountService;

    private org.web3j.tx.ClientTransactionManager getTxManager(QuorumNetworkProperty.Node node, Web3j client){
        long chainId = client.ethChainId().getId();

        String fromAddress = accountService.getDefaultAccountAddress(node).blockingFirst();
        return vanillaClientTransactionManager(client, fromAddress, chainId);
    }

    public Observable<? extends Contract> createPermissionsGenericContracts(QuorumNetworkProperty.Node node, String contractName, String upgrContractAddress, String version) {
        Web3j client = connectionFactory().getWeb3jConnection(node);

        TransactionManager transactionManager = getTxManager(node, client);

        if(version.toLowerCase().equals("v2")){
            switch (contractName.toLowerCase().trim()) {
                case "accountmanager":
                    return AccountManager.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                case "orgmanager":
                    return OrgManager.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                case "nodemanager":
                    return NodeManager.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                case "rolemanager":
                    return RoleManager.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                case "votermanager":
                    return VoterManager.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                case "permissionsupgradable":
                    return PermissionsUpgradable.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        accountService.getDefaultAccountAddress(node).blockingFirst()).flowable().toObservable();

                case "permissionsinterface":
                    return PermissionsInterface.deploy(
                        client,
                        transactionManager,
                        getPermContractGasProvider(),
                        upgrContractAddress).flowable().toObservable();

                default:
                    throw new RuntimeException("invalid contract name " + contractName);
            }
        }

        switch (contractName.toLowerCase().trim()) {
            case "accountmanager":
                return AccountManager.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            case "orgmanager":
                return OrgManager.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            case "nodemanager":
                return NodeManager.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            case "rolemanager":
                return RoleManager.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            case "votermanager":
                return VoterManager.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            case "permissionsupgradable":
                return PermissionsUpgradable.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    accountService.getDefaultAccountAddress(node).blockingFirst()).flowable().toObservable();

            case "permissionsinterface":
                return PermissionsInterface.deploy(
                    client,
                    transactionManager,
                    getPermContractGasProvider(),
                    upgrContractAddress).flowable().toObservable();

            default:
                throw new RuntimeException("invalid contract name " + contractName);
        }

    }

    public Observable<? extends Contract> createPermissionsImplementationContract(QuorumNetworkProperty.Node node, String upgrAddr, String orgMgrAddr, String roleMgrAddr, String acctMgrAddr, String voterMgrAddr, String nodeMgrAddr, String version) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        TransactionManager transactionManager = getTxManager(node, client);
        logger.debug("getPermContractDepGasProvider gas limit {}", getPermContractDepGasProvider().getGasLimit(""));
        logger.debug("getPermContractGasProvider gas limit {}", getPermContractGasProvider().getGasLimit(""));
        logger.debug("upgr:{} org:{} role:{} acct:{} vote:{} node:{}", upgrAddr, orgMgrAddr, roleMgrAddr, acctMgrAddr, voterMgrAddr, nodeMgrAddr);
        if(version.toLowerCase().equals("v2")){
            return PermissionsImplementation.deploy(
                client,
                transactionManager,
                getPermContractDepGasProvider(),
                upgrAddr,
                orgMgrAddr,
                roleMgrAddr,
                acctMgrAddr,
                voterMgrAddr,
                nodeMgrAddr
            ).flowable().toObservable();
        }
        return PermissionsImplementation.deploy(
            client,
            transactionManager,
            getPermContractGasProvider(),
            upgrAddr,
            orgMgrAddr,
            roleMgrAddr,
            acctMgrAddr,
            voterMgrAddr,
            nodeMgrAddr
        ).flowable().toObservable();

    }

    public Observable<TransactionReceipt> executeNetworkInit(QuorumNetworkProperty.Node node, String upgrAddr, String interfaceAddr, String implAddress, String version) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        TransactionManager transactionManager = getTxManager(node, client);
        if(version.toLowerCase().equals("v2")){
            return PermissionsUpgradable.load(
                upgrAddr,
                client, transactionManager,
                getPermContractGasProvider()
            ).init(interfaceAddr, implAddress).flowable().toObservable();
        }
        return PermissionsUpgradable.load(
            upgrAddr,
            client, transactionManager,
            getPermContractGasProvider()
            ).init(interfaceAddr, implAddress).flowable().toObservable();
    }

}
