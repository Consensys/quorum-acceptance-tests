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
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.ext.*;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.methods.response.PrivatePayload;
import org.web3j.tx.Contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.quorum.gauge.sol.SimpleStorage.FUNC_SET;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Service
public class TransactionService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    AccountService accountService;

    @Autowired
    PrivacyService privacyService;

    public TransactionReceipt waitForTransactionReceipt(QuorumNode node, String transactionHash) {
        Optional<TransactionReceipt> receipt = getTransactionReceipt(node, transactionHash)
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000))
            .blockingFirst().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        return receipt.get();
    }

    public Observable<EthGetTransactionReceipt> getTransactionReceipt(QuorumNode node, String transactionHash) {
        return getTransactionReceipt(networkProperty().getNode(node.name()), transactionHash);
    }

    public Observable<EthGetTransactionReceipt> getTransactionReceipt(QuorumNetworkProperty.Node node, String transactionHash) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethGetTransactionReceipt(transactionHash)
            .flowable()
            .toObservable()
            .flatMap(r -> Observable.just(
                // for PMTs: so that we don't have to duplicate all private tx tests for PMTs, we fetch the internal
                // private tx receipt which can be used to get the contract address etc.
                maybeGetPrivateTransactionReceipt(node, r).orElse(r)
            ));
    }

    private Optional<EthGetTransactionReceipt> maybeGetPrivateTransactionReceipt(QuorumNetworkProperty.Node node, EthGetTransactionReceipt publicReceiptResponse) {
        if (publicReceiptResponse.getTransactionReceipt().isEmpty()) {
            return Optional.empty();
        }

        return QuorumTransactionManagerService.maybeGetPrivateTransactionReceipt(
            connectionFactory().getWeb3jService(node),
            publicReceiptResponse.getTransactionReceipt().get()
        );
    }

    public Optional<TransactionReceipt> pollTransactionReceipt(QuorumNetworkProperty.Node node, String transactionHash) {
        for (int i = 0; i < 60; i++) {
            EthGetTransactionReceipt r = getTransactionReceipt(node, transactionHash)
                .delay(3, TimeUnit.SECONDS)
                .blockingFirst();
            if (r.getTransactionReceipt().isPresent()) {
                return r.getTransactionReceipt();
            }
        }
        return Optional.empty();
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
                    .flowable().toObservable()
                    .flatMap(ethGetTransactionCount -> {
                        Transaction tx = Transaction.createEtherTransaction(fromAddress,
                            ethGetTransactionCount.getTransactionCount(),
                            BigInteger.ZERO,
                            DEFAULT_GAS_LIMIT,
                            toAddress,
                            BigInteger.valueOf(value));
                        return client.ethSendTransaction(tx).flowable().toObservable();
                    });
            });
    }

    public Observable<EthSendTransaction> sendSignedPublicTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
            accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
            accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
            (fromAddress, toAddress) -> {
                BigInteger transactionCount = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                    .flowable().toObservable().blockingFirst().getTransactionCount();
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
                return request.flowable().toObservable();
            })
            .flatMap(ethSignTransaction -> {
                String rawHexString = ethSignTransaction.getRaw().orElseThrow();
                return client.ethSendRawTransaction(rawHexString).flowable().toObservable();
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
            .flatMap(tx -> client.ethSendTransaction(tx).flowable().toObservable());
    }

    public Observable<EthSendTransaction> sendSignedPrivateTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        return Observable.zip(
            accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
            accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
            (fromAddress, toAddress) -> {
                BigInteger transactionCount = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                    .flowable().toObservable().blockingFirst().getTransactionCount();
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
                return request.flowable().toObservable();
            })
            .flatMap(ethSignTransaction -> {
                String rawHexString = ethSignTransaction.getRaw().orElseThrow();
                return client.ethSendRawTransaction(rawHexString).flowable().toObservable();
            });
    }

    public Observable<EthSendTransaction> sendSignedPrivateTransaction(String apiMethod, String txData, QuorumNode from, QuorumNode privateFor, String targetContract) {
        Quorum quorumClient = connectionFactory().getConnection(from);

        // sleep to allow time for previous tx to be minted so that nonce is updated
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            logger.error("sleep interrupted", e);
        }

        String fromAddress = accountService.getDefaultAccountAddress(from).blockingFirst();

        BigInteger transactionCount = quorumClient.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
            .flowable().toObservable().blockingFirst().getTransactionCount();

        ExtendedPrivateTransaction tx = new ExtendedPrivateTransaction(
            fromAddress,
            transactionCount,
            BigInteger.ZERO,
            DEFAULT_GAS_LIMIT,
            targetContract,
            null,
            txData,
            null,
            Arrays.asList(privacyService.id(privateFor))
        );

        List<Object> params = new ArrayList<>(Collections.singletonList(tx));
        if ("personal_signTransaction".equals(apiMethod)) {
            // add empty password
            params.add("");
        }

        Request<?, EthSignTransaction> request = new Request<>(
            apiMethod,
            params,
            connectionFactory().getWeb3jService(from),
            EthSignTransaction.class
        );

        Observable<EthSignTransaction> ethSignTransaction = request.flowable().toObservable();

        String rawHexString = ethSignTransaction.blockingFirst().getRaw().orElseThrow();
        logger.debug("rawHexString", rawHexString);

        return quorumClient.ethSendRawPrivateTransaction(rawHexString, Arrays.asList(privacyService.id(privateFor))).flowable().toObservable();
    }

    // Invoking eth_getQuorumPayload
    public Observable<EthGetQuorumPayload> getPrivateTransactionPayload(QuorumNode node, String transactionHash) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return client.ethGetTransactionByHash(transactionHash).flowable().toObservable()
            .flatMap(ethTransaction -> Observable.just(ethTransaction.getTransaction().orElseThrow(() -> new RuntimeException("no such transaction")).getInput()))
            .flatMap(payloadHash -> {
                Request<?, EthGetQuorumPayload> request = new Request<>(
                    "eth_getQuorumPayload",
                    Arrays.asList(payloadHash),
                    connectionFactory().getWeb3jService(node),
                    EthGetQuorumPayload.class
                );
                return request.flowable().toObservable();
            });
    }

    // Delegate for eth_getQuorumPayload
    public Observable<PrivatePayload> getQuorumPayload(QuorumNode node, String encryptedPayloadHash) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumGetPrivatePayload(encryptedPayloadHash).flowable().toObservable();
    }

    // Invoking eth_getLogs
    public Observable<EthLog> getLogsUsingFilter(QuorumNode node, String contractAddress) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameter.valueOf("latest"), contractAddress);

        return client.ethGetLogs(filter).flowable().toObservable();
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
                Transaction tx = Transaction.createEtherTransaction(fromAddress,
                    null, // TODO ricardolyn: should we really not send nonce?
                    BigInteger.ZERO,
                    DEFAULT_GAS_LIMIT,
                    toAddress,
                    BigInteger.valueOf(value));
                return client.ethEstimateGas(tx).flowable().toObservable();
            });
    }

    // encodedData is the encoded smart contract binary + the encoded parameter list
    public Observable<EthEstimateGas> estimateGasForPublicContract(QuorumNetworkProperty.Node from, String encodedData) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        Transaction tx = Transaction.createContractTransaction(fromAddress,
            null, // TODO ricardolyn: should we really not send nonce?
            BigInteger.ZERO,
            DEFAULT_GAS_LIMIT,
            BigInteger.ZERO,
            encodedData);
        return client.ethEstimateGas(tx).flowable().toObservable();
    }

    // encodedData is the encoded smart contract binary + the encoded parameter list
    public Observable<EthEstimateGas> estimateGasForPrivateContract(QuorumNode from, QuorumNode privateFor, String encodedData) {
        Web3j client = connectionFactory().getWeb3jConnection(from);
        String fromAddress;
        try {
            fromAddress = client.ethCoinbase().send().getAddress();
        } catch (IOException e) {
            logger.error("Unable to get default account for node " + from, e);
            throw new RuntimeException(e);
        }

        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(0)));
        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
            .flowable().toObservable()
            .flatMap(ethGetTransactionCount -> {
                Transaction tx = new PrivateTransaction(
                    fromAddress,
                    ethGetTransactionCount.getTransactionCount(),
                    DEFAULT_GAS_LIMIT,
                    null,
                    BigInteger.ZERO,
                    encodedData,
                    null,
                    Arrays.asList(privacyService.id(privateFor))
                );
                return client.ethEstimateGas(tx).flowable().toObservable();
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
            Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(99)),
            Collections.emptyList());
        String data = FunctionEncoder.encode(function);

        Transaction tx = Transaction.createFunctionCallTransaction(
            fromAddress,
            null, // TODO ricardolyn: should we really not send nonce?
            BigInteger.ZERO,
            DEFAULT_GAS_LIMIT,
            c.getContractAddress(),
            BigInteger.ZERO,
            data);
        return client.ethEstimateGas(tx).flowable().toObservable();
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
            Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(99)),
            Collections.emptyList());
        String data = FunctionEncoder.encode(function);

        return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
            .flowable().toObservable()
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
                return client.ethEstimateGas(tx).flowable().toObservable();
            });
    }

    public Observable<EthSignTransaction> personalSignTransaction(QuorumNetworkProperty.Node node, Transaction toSign, String acctPwd) {
        List<Object> params = new ArrayList<>();
        params.add(toSign);
        params.add(acctPwd);

        Request<?, EthSignTransaction> request = new Request<>(
            "personal_signTransaction",
            params,
            connectionFactory().getWeb3jService(node),
            EthSignTransaction.class);

        return request.flowable().toObservable();
    }

    public Observable<StringResponse> personalSign(QuorumNetworkProperty.Node node, String toSign, String from, String acctPwd) {
        List<Object> params = new ArrayList<>();
        params.add(toSign);
        params.add(from);
        params.add(acctPwd);

        Request<?, StringResponse> request = new Request<>(
            "personal_sign",
            params,
            connectionFactory().getWeb3jService(node),
            StringResponse.class);

        return request.flowable().toObservable();
    }

}
