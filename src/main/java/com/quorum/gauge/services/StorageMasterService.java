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

import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthChainId;
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.sol.SimpleStorage;
import com.quorum.gauge.sol.StorageMaster;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.tx.Contract;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageMasterService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(StorageMasterService.class);

    @Autowired
    protected RPCService rpcService;

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createStorageMasterContract(QuorumNode source, List<QuorumNode> targets, BigInteger gas, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        final List<String> privateFor;
        if (null != targets) {
            privateFor = targets.stream().filter(q -> q != null).map(q -> privacyService.id(q)).collect(Collectors.toList());
        } else {
            privateFor = null;
        }

        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager
                = new PrivateClientTransactionManager(client, address, null, privateFor, flags);
            return StorageMaster.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createSimpleStorageFromStorageMaster(final QuorumNode source,
                                                                               final List<QuorumNode> target,
                                                                               final String contractAddress,
                                                                               final BigInteger gasLimit,
                                                                               final int newValue,
                                                                               final List<PrivacyFlag> flags) {
        final Quorum client = connectionFactory().getConnection(source);
        final List<String> privateFor = target.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        return accountService.getDefaultAccountAddress(source).flatMap(acctAddress -> {
            PrivateClientTransactionManager txManager
                = new PrivateClientTransactionManager(client, acctAddress, null, privateFor, flags);
            return createSSFromSM(txManager, client, contractAddress, gasLimit, newValue);
        });
    }

    private Observable<SimpleStorage> createSSFromSM(final TransactionManager txManager,
                                                     final Quorum client,
                                                     final String contractAddress,
                                                     final BigInteger gasLimit,
                                                     final int newValue) {
        final BigInteger value = BigInteger.valueOf(newValue);

        StorageMaster storageMaster = StorageMaster.load(
            contractAddress, client, txManager, BigInteger.ZERO, gasLimit);

        return storageMaster.createSimpleStorage(value).flowable().toObservable().flatMap(receipt -> {
            final List<StorageMaster.ContractCreatedEventResponse> contractCreatedEvents = storageMaster.getContractCreatedEvents(receipt);

            if (contractCreatedEvents.size() < 1) {
                throw new RuntimeException("StorageMaster createSimpleStorage Receipt has no logs");
            }

            StorageMaster.ContractCreatedEventResponse contractCreatedEvent = contractCreatedEvents.get(0);

            SimpleStorage simpleStorage = SimpleStorage.load(contractCreatedEvent._addr, client, txManager, BigInteger.ZERO, gasLimit);
            simpleStorage.setTransactionReceipt(receipt);

            return Observable.just(simpleStorage);
        });
    }

    public Observable<TransactionReceipt> createSimpleStorageC2C3FromStorageMaster(final QuorumNode source,
                                                                                   final List<QuorumNode> target,
                                                                                   final String contractAddress,
                                                                                   final BigInteger gasLimit,
                                                                                   final int newValue,
                                                                                   final List<PrivacyFlag> flags) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);
        final List<String> privateFor = target.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        return accountService.getDefaultAccountAddress(source).flatMap(acctAdress -> {
            PrivateClientTransactionManager txManager
                = new PrivateClientTransactionManager(client, acctAdress, null, privateFor, flags);
            StorageMaster storageMaster = StorageMaster.load(contractAddress, client, txManager, BigInteger.ZERO, gasLimit);
            return storageMaster.createSimpleStorageC2C3(value).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> setC2C3ValueFromStorageMaster(final QuorumNode source,
                                                                        final List<QuorumNode> target,
                                                                        final String contractAddress,
                                                                        final BigInteger gasLimit,
                                                                        final int newValue,
                                                                        final List<PrivacyFlag> flags) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);
        final List<String> privateFor = target.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        return accountService.getDefaultAccountAddress(source).flatMap(acctAddress -> {
            PrivateClientTransactionManager txManager
                = new PrivateClientTransactionManager(client, acctAddress, null, privateFor, flags);
            StorageMaster storageMaster = StorageMaster.load(
                contractAddress, client, txManager, BigInteger.ZERO, gasLimit);
            return storageMaster.setC2C3Value(value).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createStorageMasterPublicContract(QuorumNetworkProperty.Node node, BigInteger gas) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        Quorum client = connectionFactory().getConnection(node);

        return accountService.getDefaultAccountAddress(node).flatMap(address -> {
            org.web3j.tx.ClientTransactionManager txManager = vanillaClientTransactionManager(client, address, chainId);
            return StorageMaster.deploy(client,
                txManager,
                BigInteger.valueOf(0),
                gas).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createSimpleStorageFromStorageMasterPublic(final QuorumNetworkProperty.Node node,
                                                                                     final String contractAddress,
                                                                                     final BigInteger gasLimit,
                                                                                     final int newValue) {
        final Quorum client = connectionFactory().getConnection(node);
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        return accountService.getDefaultAccountAddress(node).flatMap(acctAddress -> {
            org.web3j.tx.ClientTransactionManager txManager = vanillaClientTransactionManager(client, acctAddress, chainId);
            return createSSFromSM(txManager, client, contractAddress, gasLimit, newValue);
        });
    }


    public int readStorageMasterC2C3Value(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return StorageMaster.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).getC2C3Value().send().intValue();
        } catch (ContractCallException cce) {
            if (cce.getMessage().contains("Empty value (0x)")) {
                return 0;
            }
            logger.error("readStorageMasterC2C3Value()", cce);
            throw new RuntimeException(cce);
        } catch (Exception e) {
            logger.error("readStorageMasterC2C3Value()", e);
            throw new RuntimeException(e);
        }
    }
}
