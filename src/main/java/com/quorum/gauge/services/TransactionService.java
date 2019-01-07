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
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.quorum.gauge.ext.EthSignTransaction;
import com.quorum.gauge.ext.ExtendedPrivateTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.quorum.gauge.sol.SimpleStorage.FUNC_SET;


@Service
public class TransactionService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    AccountService accountService;

    @Autowired
    PrivacyService privacyService;

    public Observable<EthGetTransactionReceipt> getTransactionReceipt(QuorumNode node, String transactionHash) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethGetTransactionReceipt(transactionHash).observable();
    }

    public Observable<EthSendTransaction> sendPublicTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> Arrays.asList(fromAddress, toAddress))
                .flatMap(l -> {
                    String fromAddress = l.get(0);
                    String toAddress = l.get(1);
                    return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                            .observable()
                            .flatMap(ethGetTransactionCount -> {
                                Transaction tx = Transaction.createEtherTransaction(fromAddress,
                                        ethGetTransactionCount.getTransactionCount(),
                                        BigInteger.ZERO,
                                        DEFAULT_GAS_LIMIT,
                                        toAddress,
                                        BigInteger.valueOf(value));
                                return client.ethSendTransaction(tx).observable();
                            });
                });
    }

    public Observable<EthSendTransaction> sendSignedPublicTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> {
                    BigInteger transactionCount = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).observable().toBlocking().first().getTransactionCount();
                    return Transaction.createEtherTransaction(fromAddress,
                            transactionCount,
                            BigInteger.ZERO,
                            DEFAULT_GAS_LIMIT,
                            toAddress,
                            BigInteger.valueOf(value));
                })
                .flatMap(tx -> {
                    Request<?, EthSignTransaction> request = new Request<>(
                            "eth_signTransaction",
                            Arrays.asList(tx),
                            connectionFactory().getWeb3jService(from),
                            EthSignTransaction.class
                    );
                    return request.observable();
                })
                .flatMap(ethSignTransaction -> {
                    Map<String, Object> response = ethSignTransaction.getResult();
                    logger.debug("{}", response);
                    String rawHexString = (String) response.get("raw");
                    return client.ethSendRawTransaction(rawHexString).observable();
                });
    }

    public Observable<EthSendTransaction> sendPrivateTransaction(int value, QuorumNode from, QuorumNode to) {
        Quorum client = connectionFactory().getConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> new ExtendedPrivateTransaction(
                        fromAddress,
                        null,
                        BigInteger.ZERO,
                        DEFAULT_GAS_LIMIT,
                        toAddress,
                        BigInteger.valueOf(value),
                        null,
                        null,
                        Arrays.asList(privacyService.id(to))
                ))
                .flatMap(tx -> client.ethSendTransaction(tx).observable());
    }

    public Observable<EthSendTransaction> sendSignedPrivateTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> {
                    BigInteger transactionCount = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).observable().toBlocking().first().getTransactionCount();
                    return new ExtendedPrivateTransaction(
                            fromAddress,
                            transactionCount,
                            BigInteger.ZERO,
                            DEFAULT_GAS_LIMIT,
                            toAddress,
                            BigInteger.valueOf(value),
                            null,
                            null,
                            Arrays.asList(privacyService.id(to))
                    );
                })
                .flatMap(tx -> {
                    Request<?, EthSignTransaction> request = new Request<>(
                            "eth_signTransaction",
                            Arrays.asList(tx),
                            connectionFactory().getWeb3jService(from),
                            EthSignTransaction.class
                    );
                    return request.observable();
                })
                .flatMap(ethSignTransaction -> {
                    Map<String, Object> response = ethSignTransaction.getResult();
                    logger.debug("{}", response);
                    String rawHexString = (String) response.get("raw");
                    return client.ethSendRawTransaction(rawHexString).observable();
                });
    }

    // Invoking eth_getQuorumPayload
    public Observable<EthGetQuorumPayload> getPrivateTransactionPayload(QuorumNode node, String transactionHash) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return client.ethGetTransactionByHash(transactionHash).observable()
                .flatMap(ethTransaction -> Observable.just(ethTransaction.getTransaction().orElseThrow(() -> new RuntimeException("no such transaction")).getInput()))
                .flatMap(payloadHash -> {
                    Request<?, EthGetQuorumPayload> request = new Request<>(
                            "eth_getQuorumPayload",
                            Arrays.asList(payloadHash),
                            connectionFactory().getWeb3jService(node),
                            EthGetQuorumPayload.class
                    );
                    return request.observable();
                });
    }

    // Invoking eth_getLogs
    public Observable<EthLog> getLogsUsingFilter(QuorumNode node, String contractAddress) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameter.valueOf("latest"), contractAddress);

        return client.ethGetLogs(filter).observable();
    }

    public Observable<EthEstimateGas> estimateGasForTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> Arrays.asList(fromAddress, toAddress))
                .flatMap(l -> {
                    String fromAddress = l.get(0);
                    String toAddress = l.get(1);
                    return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                            .observable()
                            .flatMap(ethGetTransactionCount -> {
                                Transaction tx = Transaction.createEtherTransaction(fromAddress,
                                        ethGetTransactionCount.getTransactionCount(),
                                        BigInteger.ZERO,
                                        DEFAULT_GAS_LIMIT,
                                        toAddress,
                                        BigInteger.valueOf(value));
                                return client.ethEstimateGas(tx).observable();
                            });
                });
    }

    public Observable<EthEstimateGas> estimateGasForPublicContract(QuorumNode from, Contract c) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        String data = c.getContractBinary();
        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                .observable()
                .flatMap(ethGetTransactionCount -> {
                    Transaction tx = Transaction.createContractTransaction(fromAddress,
                            ethGetTransactionCount.getTransactionCount(),
                            BigInteger.ZERO,
                            DEFAULT_GAS_LIMIT,
                            BigInteger.ZERO,
                            data);
                    return client.ethEstimateGas(tx).observable();
                });
    }

    public Observable<EthEstimateGas> estimateGasForPrivateContract(QuorumNode from, QuorumNode privateFor, Contract c) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        String data = c.getContractBinary();
        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                .observable()
                .flatMap(ethGetTransactionCount -> {
                    Transaction tx = new PrivateTransaction(
                            fromAddress,
                            ethGetTransactionCount.getTransactionCount(),
                            DEFAULT_GAS_LIMIT,
                            null,
                            BigInteger.ZERO,
                            data,
                            null,
                            Arrays.asList(privacyService.id(privateFor))
                    );
                    return client.ethEstimateGas(tx).observable();
                });
    }

    public Observable<EthEstimateGas> estimateGasForPublicContractCall(QuorumNode from, Contract c) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        //create the encoded smart contract call
        Function function = new Function(
                FUNC_SET,
                Arrays.<org.web3j.abi.datatypes.Type>asList(new org.web3j.abi.datatypes.generated.Uint256(99)),
                Collections.<TypeReference<?>>emptyList());
        String data = FunctionEncoder.encode(function);

        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                .observable()
                .flatMap(ethGetTransactionCount -> {
                    Transaction tx = Transaction.createFunctionCallTransaction(
                            fromAddress,
                            ethGetTransactionCount.getTransactionCount(),
                            BigInteger.ZERO,
                            DEFAULT_GAS_LIMIT,
                            c.getContractAddress(),
                            BigInteger.ZERO,
                            data);
                    return client.ethEstimateGas(tx).observable();
                });
    }

    public Observable<EthEstimateGas> estimateGasForPrivateContractCall(QuorumNode from, QuorumNode privateFor, Contract c) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        //create the encoded smart contract call
        Function function = new Function(
                FUNC_SET,
                Arrays.<org.web3j.abi.datatypes.Type>asList(new org.web3j.abi.datatypes.generated.Uint256(99)),
                Collections.<TypeReference<?>>emptyList());
        String data = FunctionEncoder.encode(function);

        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                .observable()
                .flatMap(ethGetTransactionCount -> {
                    Transaction tx = new PrivateTransaction(
                            fromAddress,
                            ethGetTransactionCount.getTransactionCount(),
                            DEFAULT_GAS_LIMIT,
                            c.getContractAddress(),
                            BigInteger.ZERO,
                            data,
                            null,
                            Arrays.asList(privacyService.id(privateFor))
                    );
                    return client.ethEstimateGas(tx).observable();
                });
    }

}