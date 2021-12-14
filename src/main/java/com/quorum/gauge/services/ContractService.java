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
import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthChainId;
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.ext.EthSendTransactionAsync;
import com.quorum.gauge.ext.EthStorageRoot;
import com.quorum.gauge.ext.PrivateTransactionAsync;
import com.quorum.gauge.sol.*;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.quorum.gauge.sol.SimpleStorage.FUNC_GET;
import static java.util.Collections.emptyList;

@Service
public class ContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    protected RPCService rpcService;

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createSimpleContract(int initialValue, Node source, Node target) {
        QuorumNode targetNode = null;
        if (target != null) {
            targetNode = QuorumNode.valueOf(target.getName());
        }
        return createSimpleContract(initialValue, QuorumNode.valueOf(source.getName()), targetNode, DEFAULT_GAS_LIMIT);
    }

    public Observable<? extends Contract> createPublicSimpleContract(int initialValue, Node source, String ethAccount) {
        return createSimpleContract(initialValue, QuorumNode.valueOf(source.getName()), ethAccount, null, DEFAULT_GAS_LIMIT, emptyList());
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, Node source, String ethAccount, String privateFromAlias, List<String> privateForAliases, List<PrivacyFlag> flags) {
        return createSimpleContract(initialValue, source, ethAccount, privateFromAlias, privateForAliases, flags, DEFAULT_GAS_LIMIT);
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, Node source, String ethAccount, String privateFromAliases, List<String> privateForAliases, List<PrivacyFlag> flags, BigInteger gas) {
        if (CollectionUtils.isEmpty(flags)) {
            flags = emptyList();
        }
        Quorum client = connectionFactory().getConnection(source);
        List<PrivacyFlag> finalFlags = flags;
        return accountService.getAccountAddress(source, ethAccount).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                privacyService.id(privateFromAliases),
                privateForAliases.stream().map(privacyService::id).collect(Collectors.toList()),
                finalFlags);
            return SimpleStorage.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, QuorumNode target) {
        return createSimpleContract(initialValue, source, target, DEFAULT_GAS_LIMIT);
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, QuorumNode target, BigInteger gas) {
        List<QuorumNode> targets = null;
        List<PrivacyFlag> flags = null;
        if (target != null) {
            targets = Arrays.asList(target);
            flags = Arrays.asList(PrivacyFlag.StandardPrivate);
        }

        return createSimpleContract(initialValue, source, null, targets, gas, flags);
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, String ethAccount, List<QuorumNode> targets, BigInteger gas, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        final List<String> privateFor;
        if (null != targets) {
            privateFor = targets.stream().filter(q -> q != null).map(q -> privacyService.id(q)).collect(Collectors.toList());
        } else {
            privateFor = null;
        }

        return accountService.getAccountAddress(networkProperty().getNode(source.name()), ethAccount).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                privateFor,
                flags);
            return SimpleStorage.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, String ethAccount, List<QuorumNode> targets, BigInteger gas, List<PrivacyFlag> flags, List<QuorumNode> mandatoryFor) {
        Quorum client = connectionFactory().getConnection(source);
        final List<String> privateFor;
        if (null != targets) {
            privateFor = targets.stream().filter(q -> q != null).map(q -> privacyService.id(q)).collect(Collectors.toList());
        } else {
            privateFor = null;
        }

        final List<String> mandatoryRecipients = mandatoryFor.stream().map(privacyService::id).collect(Collectors.toList());

        return accountService.getAccountAddress(networkProperty().getNode(source.name()), ethAccount).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                privateFor,
                mandatoryRecipients,
                flags);
            return SimpleStorage.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    /**
     * Need to use EthCall to manipulate the error as webj3 doesn't
     *
     * @param node
     * @param contractAddress
     * @return
     */
    public Observable<BigInteger> readSimpleContractValue(Node node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        Function function = new Function(FUNC_GET,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
            }));
        return client.ethCoinbase().flowable().toObservable()
            .map(Response::getResult)
            .flatMap(address -> {
                Request<?, EthCall> req = client.ethCall(Transaction.createEthCallTransaction(address, contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST);
                return req.flowable().toObservable();
            })
            .map(ec -> {
                if (ec.hasError()) {
                    throw new ContractCallException(ec.getError().getMessage());
                }
                List<Type> values = FunctionReturnDecoder.decode(ec.getValue(), function.getOutputParameters());
                Type result;
                if (!values.isEmpty()) {
                    result = values.get(0);
                } else {
                    throw new ContractCallException("Empty value (0x) returned from contract");
                }
                Object value = result.getValue();
                if (BigInteger.class.isAssignableFrom(value.getClass())) {
                    return (BigInteger) value;
                } else {
                    throw new ContractCallException(
                        "Unable to convert response: " + value
                            + " to expected type: " + BigInteger.class.getSimpleName());
                }
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

    public Observable<TransactionReceipt> updateSimpleContractWithMandatoryRecipients(final QuorumNode source, List<QuorumNode> target,
                                                               final String contractAddress, final int newValue, List<PrivacyFlag> flags, List<QuorumNode> mandatoryFor) {
        return this.updateSimpleContractWithGasLimit(source, target, contractAddress, DEFAULT_GAS_LIMIT, newValue, flags, mandatoryFor);
    }

    public Observable<TransactionReceipt> updateSimpleContract(final QuorumNode source, List<QuorumNode> target,
                                                               final String contractAddress, final int newValue, List<PrivacyFlag> flags) {
        return this.updateSimpleContractWithGasLimit(source, target, contractAddress, DEFAULT_GAS_LIMIT, newValue, flags, null);
    }

    public Observable<TransactionReceipt> updateSimpleContract(final QuorumNode source, final QuorumNode target,
                                                               final String contractAddress, final int newValue, List<PrivacyFlag> flags) {
        return this.updateSimpleContractWithGasLimit(source, Arrays.asList(target), contractAddress, DEFAULT_GAS_LIMIT, newValue, flags, null);
    }

    public Observable<TransactionReceipt> updateSimpleContractWithGasLimit(final QuorumNode source,
                                                                           final List<QuorumNode> target,
                                                                           final String contractAddress,
                                                                           final BigInteger gasLimit,
                                                                           final int newValue,
                                                                           final List<PrivacyFlag> flags) {
        return this.updateSimpleContractWithGasLimit(source, target, contractAddress, gasLimit, newValue, flags, emptyList());
    }

    public Observable<TransactionReceipt> updateSimpleContractWithGasLimit(final QuorumNode source,
                                                                           final List<QuorumNode> target,
                                                                           final String contractAddress,
                                                                           final BigInteger gasLimit,
                                                                           final int newValue,
                                                                           final List<PrivacyFlag> flags,
                                                                           final List<QuorumNode> mandatoryFor) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);
        final List<String> privateFor = target.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        if (Objects.isNull(mandatoryFor)) {
            return accountService.getDefaultAccountAddress(source)
                .map(address -> new PrivateClientTransactionManager(client, address, null, privateFor, flags))
                .flatMap(txManager -> SimpleStorage.load(
                    contractAddress, client, txManager, BigInteger.ZERO, gasLimit).set(value).flowable().toObservable()
                );
        }

        final List<String> mandatoryRecipients = mandatoryFor.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        return accountService.getDefaultAccountAddress(source)
            .map(address -> new PrivateClientTransactionManager(client, address, null, privateFor, mandatoryRecipients, flags))
            .flatMap(txManager -> SimpleStorage.load(
                contractAddress, client, txManager, BigInteger.ZERO, gasLimit).set(value).flowable().toObservable()
            );
    }

    public Observable<TransactionReceipt> updateSimpleStorageContract(final int newValue, final String contractAddress, final Node source,
                                                                      String ethAccount, final String privateFromAlias,
                                                                      final List<String> privateForAliases) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);

        return accountService.getAccountAddress(source, ethAccount)
            .map(address -> new PrivateClientTransactionManager(
                client,
                address,
                privacyService.id(privateFromAlias),
                privateForAliases.stream().map(privacyService::id).collect(Collectors.toList()),
                Collections.EMPTY_LIST
            ))
            .flatMap(txManager -> SimpleStorage.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).set(value).flowable().toObservable()
            );
    }

    public Observable<TransactionReceipt> updatePublicSimpleStorageContract(final int newValue,
                                                                            final String contractAddress,
                                                                            final Node source, String ethAccount) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);

        return accountService.getAccountAddress(source, ethAccount)
            .map(address -> new PrivateClientTransactionManager(
                client,
                address,
                null,
                null,
                Collections.emptyList()
            ))
            .flatMap(txManager -> SimpleStorage.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).set(value).flowable().toObservable()
            );
    }

    public Observable<TransactionReceipt> updateSimpleStorageDelegateContract(int newValue, String contractAddress, Node source, String ethAccount, String privateFromAlias, List<String> privateForAliases) {
        Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(newValue);

        return accountService.getAccountAddress(source, ethAccount)
            .map(address -> new PrivateClientTransactionManager(
                client,
                address,
                privacyService.id(privateFromAlias),
                privateForAliases.stream().map(privacyService::id).collect(Collectors.toList()),
                Collections.EMPTY_LIST
            ))
            .flatMap(txManager -> SimpleStorageDelegate.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).set(value).flowable().toObservable()
            );
    }

    public Observable<EthStorageRoot> getStorageRoot(QuorumNode node, String contractAddress) {
        Request<String, EthStorageRoot> request = new Request<>(
            "eth_storageRoot",
            Arrays.asList(contractAddress),
            connectionFactory().getWeb3jService(node),
            EthStorageRoot.class);
        return request.flowable().toObservable();
    }

    public Observable<? extends Contract> createClientReceiptSmartContract(Node node) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        Web3j client = connectionFactory().getWeb3jConnection(node);
        return accountService.getDefaultAccountAddress(node)
            .flatMap(address -> {
                org.web3j.tx.ClientTransactionManager txManager = vanillaClientTransactionManager(client, address, chainId);
                return ClientReceipt.deploy(
                    client,
                    txManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).flowable().toObservable();
            });
    }

    public int readGenericStoreContractGetValue(QuorumNode node, String contractAddress, String contractName, String methodName) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);

            switch (contractName.toLowerCase().trim()) {
                case "storea":
                    switch (methodName.toLowerCase().trim()) {
                        case "geta":
                            return Storea.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).geta().send().intValue();
                        case "getb":
                            return Storea.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).getb().send().intValue();
                        case "getc":
                            return Storea.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).getc().send().intValue();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);

                    }
                case "storeb":
                    switch (methodName.toLowerCase().trim()) {
                        case "getb":
                            return Storeb.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).getb().send().intValue();
                        case "getc":
                            return Storeb.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).getc().send().intValue();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);
                    }
                case "storec":
                    switch (methodName.toLowerCase().trim()) {
                        case "getc":
                            return Storec.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).getc().send().intValue();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);
                    }
                default:
                    throw new Exception("invalid contract name ");
            }
        } catch (Exception e) {
            logger.debug("readStoreContractValue() " + contractName + " " + methodName, e);
            throw new RuntimeException(e);
        }
    }

    public Observable<TransactionReceipt> setGenericStoreContractSetValue(QuorumNetworkProperty.Node node, String contractAddress, String contractName, String methodName, int value, boolean isPrivate, QuorumNode target, PrivacyFlag privacyType) {

        Quorum client = connectionFactory().getConnection(node);

        String fromAddress = accountService.getDefaultAccountAddress(node).blockingFirst();
        TransactionManager txManager;
        if (isPrivate) {
            txManager = new PrivateClientTransactionManager(
                client,
                fromAddress,
                null,
                Arrays.asList(privacyService.id(target)),
                Arrays.asList(privacyType));
        } else {
            txManager = new PrivateClientTransactionManager(
                client,
                fromAddress,
                null,
                null,
                List.of(PrivacyFlag.StandardPrivate));
        }
        try {
            switch (contractName.toLowerCase().trim()) {
                case "storea":
                    switch (methodName.toLowerCase().trim()) {
                        case "seta":
                            return Storea.load(contractAddress, client, txManager,

                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).seta(BigInteger.valueOf(value)).flowable().toObservable();
                        case "setb":
                            return Storea.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).setb(BigInteger.valueOf(value)).flowable().toObservable();
                        case "setc":
                            return Storea.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).setc(BigInteger.valueOf(value)).flowable().toObservable();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);

                    }
                case "storeb":
                    switch (methodName.toLowerCase().trim()) {
                        case "setb":
                            return Storeb.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).setb(BigInteger.valueOf(value)).flowable().toObservable();
                        case "setc":
                            return Storeb.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).setc(BigInteger.valueOf(value)).flowable().toObservable();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);
                    }
                case "storec":
                    switch (methodName.toLowerCase().trim()) {
                        case "setc":
                            return Storec.load(contractAddress, client, txManager,
                                BigInteger.valueOf(0),
                                DEFAULT_GAS_LIMIT).setc(BigInteger.valueOf(value)).flowable().toObservable();
                        default:
                            throw new Exception("invalid method name " + methodName + " for contract " + contractName);
                    }
                default:
                    throw new Exception("invalid contract name " + contractName);
            }
        } catch (Exception e) {
            logger.debug("setStoreContractValue() " + contractName + " " + methodName, e);
            throw new RuntimeException(e);
        }
    }

    public Observable<? extends Contract> createGenericStoreContract(QuorumNetworkProperty.Node node, String contractName, int initalValue, String dpContractAddress, boolean isPrivate, QuorumNode target, PrivacyFlag privacyType) {
        Quorum client = connectionFactory().getConnection(node);

        String fromAddress = accountService.getDefaultAccountAddress(node).blockingFirst();

        PrivateClientTransactionManager transactionManager;
        if (isPrivate) {
            transactionManager = new PrivateClientTransactionManager(
                client,
                fromAddress,
                null,
                Arrays.asList(privacyService.id(target)),
                List.of(privacyType));
        } else {
            transactionManager = new PrivateClientTransactionManager(
                client,
                fromAddress,
                null,
                null,
                List.of(PrivacyFlag.StandardPrivate));
        }
        switch (contractName.toLowerCase().trim()) {
            case "storea":
                return Storea.deploy(
                    client,
                    transactionManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT, BigInteger.valueOf(initalValue), dpContractAddress).flowable().toObservable();

            case "storeb":
                return Storeb.deploy(
                    client,
                    transactionManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT, BigInteger.valueOf(initalValue), dpContractAddress).flowable().toObservable();
            case "storec":
                return Storec.deploy(
                    client,
                    transactionManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT, BigInteger.valueOf(initalValue)).flowable().toObservable();
            default:
                throw new RuntimeException("invalid contract name " + contractName);
        }
    }

    public Observable<? extends Contract> createClientReceiptPrivateSmartContract(Node source, String ethAccount, String privateFromAlias, List<String> privateForAliases) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getAccountAddress(source, ethAccount).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = clientTransactionManager(
                client,
                address,
                privacyService.id(privateFromAlias),
                privacyService.ids(privateForAliases));
            return ClientReceipt.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createClientReceiptPrivateSmartContract(QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = clientTransactionManager(
                client,
                address,
                null,
                List.of(privacyService.id(target)));
            return ClientReceipt.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> updateClientReceipt(Node node, String contractAddress, BigInteger value) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return accountService.getDefaultAccountAddress(node)
            .flatMap(address -> {
                org.web3j.tx.ClientTransactionManager txManager = vanillaClientTransactionManager(client, address, chainId);
                return ClientReceipt.load(contractAddress, client, txManager, BigInteger.valueOf(0), DEFAULT_GAS_LIMIT)
                    .deposit(new byte[32], value).flowable().toObservable();
            });
    }

    public Observable<TransactionReceipt> updateClientReceiptPrivate(QuorumNode source, QuorumNode target, String contractAddress, BigInteger value) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager txManager = clientTransactionManager(
                client,
                address,
                null,
                List.of(privacyService.id(target)));
            return ClientReceipt.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).deposit(new byte[32], value).flowable().toObservable();
        });
    }

    public Observable<EthSendTransactionAsync> createClientReceiptContractAsync(int initialValue, QuorumNode source, String sourceAccount, QuorumNode target, String callbackUrl) {
        InputStream binaryStream = ClientReceipt.class.getResourceAsStream("/com.quorum.gauge.sol/ClientReceipt.bin");
        if (binaryStream == null) {
            throw new IllegalStateException("Can't find resource ClientReceipt.bin");
        }
        return (sourceAccount != null ? Observable.just(sourceAccount) : accountService.getDefaultAccountAddress(source))
            .flatMap(fromAddress -> {
                try {
                    String binary = StreamUtils.copyToString(binaryStream, Charset.defaultCharset());
                    PrivateTransactionAsync tx = new PrivateTransactionAsync(
                        fromAddress,
                        null,
                        DEFAULT_GAS_LIMIT,
                        null,
                        BigInteger.valueOf(0),
                        binary,
                        null,
                        Arrays.asList(privacyService.id(target)),
                        callbackUrl
                    );
                    Request<?, EthSendTransactionAsync> request = new Request<>(
                        "eth_sendTransactionAsync",
                        Arrays.asList(tx),
                        connectionFactory().getWeb3jService(source),
                        EthSendTransactionAsync.class
                    );
                    return request.flowable().toObservable();
                } catch (IOException e) {
                    logger.error("Unable to construct transaction arguments", e);
                    throw new RuntimeException(e);
                }
            });
    }

    public Observable<org.web3j.protocol.core.methods.response.EthFilter> newLogFilter(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameter.valueOf("latest"), contractAddress);
        return client.ethNewFilter(filter).flowable().toObservable();
    }

    public Observable<EthUninstallFilter> uninstallFilter(QuorumNode node, BigInteger filterId) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethUninstallFilter(filterId).flowable().toObservable();
    }

    public Observable<EthLog> getFilterLogs(QuorumNode node, BigInteger filterId) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethGetFilterLogs(filterId).flowable().toObservable();
    }

    public Observable<? extends Contract> createSimpleDelegatePrivateContract(String delegateContractAddress, Node source, String ethAccount, String privateFromAlias, List<String> privateForAliases) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getAccountAddress(source, ethAccount)
            .flatMap(address -> {
                ClientTransactionManager clientTransactionManager = clientTransactionManager(
                    client,
                    address,
                    privacyService.id(privateFromAlias),
                    privacyService.ids(privateForAliases));
                return SimpleStorageDelegate.deploy(client,
                        clientTransactionManager,
                        BigInteger.valueOf(0),
                        DEFAULT_GAS_LIMIT,
                        delegateContractAddress)
                    .flowable().toObservable();
            });
    }

    public Observable<EthGetCode> getCode(Node node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST)
            .flowable().toObservable();
    }
}
