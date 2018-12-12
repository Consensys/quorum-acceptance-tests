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

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthSendTransactionAsync;
import com.quorum.gauge.ext.EthStorageRoot;
import com.quorum.gauge.ext.PrivateTransactionAsync;
import com.quorum.gauge.sol.ClientReceipt;
import com.quorum.gauge.sol.SimpleStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.exceptions.ContractCallException;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

@Service
public class ContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, QuorumNode target) {
        return createSimpleContract(initialValue, source, target, DEFAULT_GAS_LIMIT);
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, QuorumNode target, BigInteger gas) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new ClientTransactionManager(
                    client,
                    address,
                    null,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return SimpleStorage.deploy(client,
                    clientTransactionManager,
                    BigInteger.valueOf(0),
                    gas,
                    BigInteger.valueOf(initialValue)).observable();
        });
    }

    // Read-only contract
    public int readSimpleContractValue(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return SimpleStorage.load(contractAddress, client, txManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).get().send().intValue();
        } catch (ContractCallException cce) {
            if (cce.getMessage().contains("Empty value (0x)")) {
                return 0;
            }
            logger.error("readSimpleContractValue()", cce);
            throw new RuntimeException(cce);
        } catch (Exception e) {
            logger.error("readSimpleContractValue()", e);
            throw new RuntimeException(e);
        }
    }

    public Observable<TransactionReceipt> updateSimpleContract(QuorumNode source, QuorumNode target, String contractAddress, int newValue) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager txManager = new ClientTransactionManager(
                    client,
                    address,
                    null,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return SimpleStorage.load(contractAddress, client, txManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).observable();
        });
    }

    public Observable<EthStorageRoot> getStorageRoot(QuorumNode node, String contractAddress) {
        Request<String, EthStorageRoot> request = new Request<>(
                "eth_storageRoot",
                Arrays.asList(contractAddress),
                connectionFactory().getWeb3jService(node),
                EthStorageRoot.class);
        return request.observable();
    }

    public Observable<? extends Contract> createClientReceiptSmartContract(QuorumNode node) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return accountService.getDefaultAccountAddress(node)
                .flatMap(address -> {
                    org.web3j.tx.ClientTransactionManager txManager = new org.web3j.tx.ClientTransactionManager(
                            client,
                            address,
                            DEFAULT_MAX_RETRY,
                            DEFAULT_SLEEP_DURATION_IN_MILLIS);
                    return ClientReceipt.deploy(
                            client,
                            txManager,
                            BigInteger.valueOf(0),
                            DEFAULT_GAS_LIMIT).observable();
                });
    }

    public Observable<? extends Contract> createClientReceiptPrivateSmartContract(QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new ClientTransactionManager(
                    client,
                    address,
                    null,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return ClientReceipt.deploy(client,
                    clientTransactionManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).observable();
        });
    }

    public Observable<TransactionReceipt> updateClientReceipt(QuorumNode node, String contractAddress, BigInteger value) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return accountService.getDefaultAccountAddress(node)
                .flatMap(address -> {
                    org.web3j.tx.ClientTransactionManager txManager = new org.web3j.tx.ClientTransactionManager(
                            client,
                            address,
                            DEFAULT_MAX_RETRY,
                            DEFAULT_SLEEP_DURATION_IN_MILLIS);
                    return ClientReceipt.load(contractAddress, client, txManager, BigInteger.valueOf(0), DEFAULT_GAS_LIMIT)
                            .deposit(new byte[32], value).observable();
                });
    }

    public Observable<TransactionReceipt> updateClientReceiptPrivate(QuorumNode source, QuorumNode target, String contractAddress, BigInteger value) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager txManager = new ClientTransactionManager(
                    client,
                    address,
                    null,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return ClientReceipt.load(contractAddress, client, txManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).deposit(new byte[32], value).observable();
        });
    }

    public Observable<EthSendTransactionAsync> createClientReceiptContractAsync(int initialValue, QuorumNode source, String sourceAccount, QuorumNode target, String callbackUrl) {
        InputStream binaryStream = ClientReceipt.class.getResourceAsStream("/com.quorum.gauge.sol/ClientReceipt.bin");
        if (binaryStream == null) {
            throw new IllegalStateException("Can't find resource ClientReceipt.bin");
        }
        return Observable.zip(
                sourceAccount != null ? Observable.just(sourceAccount) : accountService.getDefaultAccountAddress(source).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(target).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> {
                    try {
                        String binary = StreamUtils.copyToString(binaryStream, Charset.defaultCharset());
                        return new PrivateTransactionAsync(
                                fromAddress,
                                null,
                                DEFAULT_GAS_LIMIT,
                                toAddress,
                                BigInteger.valueOf(0),
                                binary,
                                null,
                                Arrays.asList(privacyService.id(target)),
                                callbackUrl
                        );
                    } catch (IOException e) {
                        logger.error("Unable to construct transaction arguments", e);
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(tx -> {
                    Request<?, EthSendTransactionAsync> request = new Request<>(
                            "eth_sendTransactionAsync",
                            Arrays.asList(tx),
                            connectionFactory().getWeb3jService(source),
                            EthSendTransactionAsync.class
                    );
                    return request.observable();
                });
    }
}
