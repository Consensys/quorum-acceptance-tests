package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
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

import java.math.BigInteger;

@Service
public class PermissionsContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    AccountService accountService;

    private org.web3j.tx.ClientTransactionManager getTxManager(QuorumNetworkProperty.Node node, Web3j client){
        String fromAddress = accountService.getDefaultAccountAddress(node).blockingFirst();
        return new org.web3j.tx.ClientTransactionManager(
            client,
            fromAddress,
            DEFAULT_MAX_RETRY,
            DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public Observable<? extends Contract> createPermissionsGenericContracts(QuorumNetworkProperty.Node node, String contractName, String upgrContractAddress) {
        Web3j client = connectionFactory().getWeb3jConnection(node);

        TransactionManager transactionManager = getTxManager(node, client);

        switch (contractName.toLowerCase().

            trim()) {
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

    public Observable<? extends Contract> createPermissionsImplementationContract(QuorumNetworkProperty.Node node, String upgrAddr, String orgMgrAddr, String roleMgrAddr, String acctMgrAddr, String voterMgrAddr, String nodeMgrAddr) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        TransactionManager transactionManager = getTxManager(node, client);

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

    public Observable<TransactionReceipt> executeNetworkInit(QuorumNetworkProperty.Node node, String upgrAddr, String interfaceAddr, String implAddress) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        TransactionManager transactionManager = getTxManager(node, client);

        return PermissionsUpgradable.load(
            upgrAddr,
            client, transactionManager,
            getPermContractGasProvider()
            ).init(interfaceAddr, implAddress).flowable().toObservable();
    }

}
