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
import com.quorum.gauge.common.RawDeployedContractTarget;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.ext.filltx.FillTransactionResponse;
import com.quorum.gauge.ext.filltx.PrivateFillTransaction;
import com.quorum.gauge.sol.ClientReceipt;
import com.quorum.gauge.sol.ContractExtenderVoting;
import com.quorum.gauge.sol.SimpleStorage;
import com.quorum.gauge.sol.SimpleStorageDelegate;
import com.quorum.gauge.sol.SneakyWrapper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.UnixDomainSocketFactory;
import org.web3j.quorum.enclave.Constellation;
import org.web3j.quorum.enclave.Enclave;
import org.web3j.quorum.enclave.SendResponse;
import org.web3j.quorum.enclave.Tessera;
import org.web3j.quorum.enclave.protocol.EnclaveService;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RawContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(RawContractService.class);

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    OkHttpClient httpClient;

    public Observable<? extends Contract> createRawSimplePublicContract(int initialValue, WalletData wallet, QuorumNetworkProperty.Node node, long chainId) {
        Web3j web3j = connectionFactory().getWeb3jConnection(node);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            RawTransactionManager qrtxm = rawTransactionManager(web3j, credentials, chainId);

            return SimpleStorage.deploy(web3j,
                qrtxm,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();

        } catch (IOException e) {
            logger.error("RawTransaction- public", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - public - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<TransactionReceipt> updateRawSimplePublicContract(QuorumNetworkProperty.Node node, WalletData wallet, String contractAddress, int newValue, final long chainId) {
        Web3j web3j = connectionFactory().getWeb3jConnection(node);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            RawTransactionManager qrtxm = rawTransactionManager(web3j, credentials, chainId);

            return SimpleStorage.load(contractAddress, web3j,
                qrtxm,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).flowable().toObservable();

        } catch (IOException e) {
            logger.error("RawTransaction- public", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - public - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<? extends Contract> createRawSimplePrivateContract(int initialValue, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SimpleStorage.deploy(client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT,
                    BigInteger.valueOf(initialValue))
                .flowable().toObservable());
    }

    public Observable<TransactionReceipt> updateRawSimplePrivateContract(int newValue, String contractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SimpleStorage.load(contractAddress, client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue))
                .flowable().toObservable());
    }

    public Observable<? extends Contract> createRawSimplePrivateContract(int initialValue, WalletData wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            TransactionManager qrtxm = quorumTransactionManager(client,
                credentials,
                privacyService.id(source),
                List.of(privacyService.id(target)),
                enclave);

            return SimpleStorage.deploy(client,
                qrtxm,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();

        } catch (IOException e) {
            logger.error("RawTransaction - private", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - private - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<TransactionReceipt> doVoteManagementContract(Boolean vote, String nextUuid, String contractAddress, WalletData wallet, QuorumNetworkProperty.Node source, QuorumNetworkProperty.Node target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(QuorumNode.valueOf(source.getName()), client);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            TransactionManager qrtxm = quorumTransactionManager(client,
                credentials,
                privacyService.id(source),
                List.of(privacyService.id(target)),
                enclave);

            return ContractExtenderVoting.load(contractAddress, client,
                qrtxm,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).doVote(vote, nextUuid).flowable().toObservable();

        } catch (IOException e) {
                logger.error("RawTransaction - private", e);
                return Observable.error(e);
            } catch (CipherException e) {
                logger.error("RawTransaction - private - bad credentials", e);
                return Observable.error(e);
        }
    }

    public Observable<TransactionReceipt> updateRawSimplePrivateContract(int newValue, String contractAddress, WalletData wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            TransactionManager qrtxm = quorumTransactionManager(client,
                credentials,
                privacyService.id(source),
                List.of(privacyService.id(target)),
                enclave);

            return SimpleStorage.load(contractAddress, client,
                qrtxm,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).flowable().toObservable();

        } catch (IOException e) {
            logger.error("RawTransaction - private", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - private - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<EthSendTransaction> createRawSimplePrivateContractUsingEthApi(String apiMethod, int initialValue, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        String payload = base64SimpleStorageConstructorBytecode(initialValue);
        SendResponse storeRawResponse = enclave.storeRawRequest(
            payload,
            privacyService.id(source),
            Collections.emptyList()
        );

        String tmHash = base64ToHex(storeRawResponse.getKey());
        return transactionService.sendSignedPrivateTransaction(apiMethod, tmHash, source, target, null);
    }

    public Observable<EthGetTransactionReceipt> updateRawSimplePrivateContractUsingEthApi(String apiMethod, int newValue, String contractAddress, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        String payload = base64SimpleStorageSetBytecode(newValue);
        SendResponse storeRawResponse = enclave.storeRawRequest(
            payload,
            privacyService.id(source),
            Collections.emptyList()
        );

        String tmHash = base64ToHex(storeRawResponse.getKey());
        EthSendTransaction sendTransactionResponse = transactionService.sendSignedPrivateTransaction(apiMethod, tmHash, source, target, contractAddress).blockingFirst();

        Optional<String> responseError = Optional.ofNullable(sendTransactionResponse.getError()).map(Response.Error::getMessage);
        responseError.ifPresent(e -> logger.error("EthSendTransaction error: {}", e));

        logger.debug("sent tx: {}", sendTransactionResponse.getTransactionHash());

        return transactionService.getTransactionReceipt(source, sendTransactionResponse.getTransactionHash())
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000));
    }

    public Observable<FillTransactionResponse> fillTransaction(QuorumNode from, QuorumNode to, int initValue) {
        String data = base64ToHex(base64SimpleStorageConstructorBytecode(initValue));
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> new PrivateTransaction(
                    fromAddress,
                    null,
                    DEFAULT_GAS_LIMIT,
                    toAddress,
                    BigInteger.ZERO,
                    data,
                    null,
                    Arrays.asList(privacyService.id(to))
                ))
            .flatMap(tx -> {
                Request<?, FillTransactionResponse> request = new Request<>(
                    "eth_fillTransaction",
                    Collections.singletonList(tx),
                    connectionFactory().getWeb3jService(from),
                    FillTransactionResponse.class
                );
                return request.flowable().toObservable();
            });
    }

    public Observable<FillTransactionResponse> signTransaction(QuorumNode from, PrivateFillTransaction tx) {
        Web3j client = connectionFactory().getConnection(from);

        Request<?, FillTransactionResponse> request = new Request<>(
            "eth_signTransaction",
            Collections.singletonList(tx),
            connectionFactory().getWeb3jService(from),
            FillTransactionResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<EthSendTransaction> sendRawPrivateTransaction(QuorumNode from, String rawHexString, QuorumNode privateFor) {
        Quorum quorumClient = connectionFactory().getConnection(from);

        return quorumClient.ethSendRawPrivateTransaction(rawHexString, Arrays.asList(privacyService.id(privateFor))).flowable().toObservable();
    }


    private String base64SimpleStorageConstructorBytecode(int initialValue) {
        final InputStream binaryStream = SimpleStorage.class.getResourceAsStream("/com.quorum.gauge.sol/SimpleStorage.bin");
        if (binaryStream == null) {
            throw new IllegalStateException("Can't find resource SimpleStorage.bin");
        }

        final String binary;
        try {
            binary = StreamUtils.copyToString(binaryStream, Charset.defaultCharset());
        } catch (IOException e) {
            logger.error("Unable to parse contents of SimpleStorage.bin", e);
            throw new RuntimeException(e);
        }

        final String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(initialValue)));
        final String constructorWithArgs = binary + encodedConstructor;

        return Base64.getEncoder().encodeToString(
            Numeric.hexStringToByteArray(constructorWithArgs)
        );
    }

    private String base64SimpleStorageSetBytecode(int newValue) {
        final Function function = new Function(
            SimpleStorage.FUNC_SET,
            Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(newValue)),
            Collections.emptyList());

        final String encodedSet = FunctionEncoder.encode(function);

        return Base64.getEncoder().encodeToString(
            Numeric.hexStringToByteArray(encodedSet)
        );
    }

    private String base64ToHex(String b64) {
        byte[] raw = Base64.getDecoder().decode(b64);
        return Numeric.toHexString(raw);
    }

    private Enclave buildEnclave(QuorumNode source, Quorum client) {
        String thirdPartyURL = privacyService.thirdPartyUrl(source);
        if (thirdPartyURL.endsWith("ipc")) {
            EnclaveService enclaveService = new EnclaveService("http://localhost", 12345, getIPCHttpClient(thirdPartyURL));
            return new Constellation(enclaveService, client);
        } else {
            URI uri = URI.create(thirdPartyURL);
            String enclaveUrl = uri.getScheme() + "://" + uri.getHost();
            EnclaveService enclaveService = new EnclaveService(enclaveUrl, uri.getPort(), httpClient);
            return new Tessera(enclaveService, client);
        }
    }

    private Map<String, OkHttpClient> ipcClients = new ConcurrentHashMap<>();

    private OkHttpClient getIPCHttpClient(String thirdPartyURL) {
        if (ipcClients.containsKey(thirdPartyURL)) {
            return ipcClients.get(thirdPartyURL);
        }
        OkHttpClient client = new OkHttpClient.Builder()
            .socketFactory(new UnixDomainSocketFactory(new File(thirdPartyURL)))
            .build();

        ipcClients.put(thirdPartyURL, client);

        return client;
    }

    public Observable<? extends Contract> createRawClientReceiptPrivateContract(WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> ClientReceipt.deploy(client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT)
                .flowable().toObservable());
    }

    public Observable<TransactionReceipt> updateRawClientReceiptPrivateContract(String contractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> ClientReceipt.load(contractAddress, client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).deposit(new byte[32], BigInteger.ZERO)
                .flowable().toObservable());
    }

    public Observable<? extends Contract> createRawSimpleDelegatePrivateContract(String delegateContractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        logger.debug("Create SimpleStorageDelegateContract({}) using a private raw transaction", delegateContractAddress);
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SimpleStorageDelegate.deploy(client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT,
                    delegateContractAddress)
                .flowable().toObservable());
    }

    public Observable<TransactionReceipt> updateRawSimpleDelegatePrivateContract(int newValue, String contractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        logger.debug("Update SimpleStorageDelegateContract@{} via a private raw transaction", contractAddress);
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SimpleStorageDelegate.load(contractAddress, client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue))
                .flowable().toObservable());
    }


    public Observable<? extends Contract> createRawSneakyWrapperPrivateContract(String delegateContractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        logger.debug("Create SimpleStorageDelegateContract({}) using a private raw transaction", delegateContractAddress);
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SneakyWrapper.deploy(client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT,
                    delegateContractAddress)
                .flowable().toObservable());
    }

    public Observable<TransactionReceipt> updateDelegateInSneakyWrapperContract(boolean newValue, String contractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(quorumTransactionManager(client,
                cred,
                privacyService.id(source, sourceNamedKey),
                privacyService.ids(targetNamedKeys),
                enclave)))
            .flatMap(qrtxm -> SneakyWrapper.load(contractAddress, client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).setDelegate(newValue)
                .flowable().toObservable());
    }

    public Observable<TransactionReceipt> invokeGetFromDelegateInSneakyWrapper(int nonceShift, String contractAddress, WalletData wallet, QuorumNode source, String sourceNamedKey, List<String> targetNamedKeys) {
        logger.debug("Update SimpleStorageDelegateContract@{} via a private raw transaction", contractAddress);
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        return Observable.fromCallable(() -> wallet)
            .flatMap(walletData -> Observable.fromCallable(() -> WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath())))
            .flatMap(cred -> Observable.just(new PrivateClientTransactionManager(client,
                cred.getAddress(),
                privacyService.id(source, sourceNamedKey),
                targetNamedKeys.stream().map(privacyService::id).collect(Collectors.toList())
            )))
            .flatMap(qrtxm -> SneakyWrapper.load(contractAddress, client,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).getFromDelegate()
                .flowable().toObservable());
    }

    public int invokeGetInSneakyWrapper(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return SneakyWrapper.load(contractAddress, client, txManager,
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

    public List<Observable<RawDeployedContractTarget>> createNRawSimplePrivateContract(int count, WalletData wallet, QuorumNode source, QuorumNode[] targetNodes) throws IOException, CipherException {
        try {
            Quorum client = connectionFactory().getConnection(source);
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());
            String fromAddress = credentials.getAddress();
            BigInteger transactionCount = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).flowable().toObservable().blockingFirst().getTransactionCount();
            Enclave enclave = buildEnclave(source, client);
            List<Observable<RawDeployedContractTarget>> allObservableContracts = new ArrayList<>();

            int counter = 0;
            for (QuorumNode targetNode : targetNodes) {
                RawPrivateContract[] rawPrivateContracts = new RawPrivateContract[count];
                for (int j = 0; j < count; j++) {
                    int arbitraryValue = new Random().nextInt(50) + 1;
                    String payload = base64SimpleStorageConstructorBytecode(arbitraryValue);
                    SendResponse storeRawResponse = enclave.storeRawRequest(
                        payload,
                        privacyService.id(source),
                        Collections.emptyList()
                    );
                    String tmHash = base64ToHex(storeRawResponse.getKey());
                    RawTransaction tx = RawTransaction.createContractTransaction(
                        transactionCount.add(BigInteger.valueOf(counter)),
                        BigInteger.ZERO,
                        DEFAULT_GAS_LIMIT,
                        BigInteger.ZERO,
                        tmHash
                    );
                    counter++;
                    rawPrivateContracts[j] = new RawPrivateContract(sign(tx, credentials), arbitraryValue, targetNode);
                }
                allObservableContracts.add(Observable.fromArray(rawPrivateContracts)
                    .flatMap(raw -> sendRawPrivateTransaction(source, raw.rawTransaction, targetNode)
                        .map(b -> transactionService.waitForTransactionReceipt(targetNode, b.getTransactionHash()))
                        .map(receipt -> new RawDeployedContractTarget(raw.value, raw.node, receipt))
                        .subscribeOn(Schedulers.single())));
            }
            return allObservableContracts;
        } catch (IOException e) {
            logger.error("RawTransaction - private", e);
            throw e;
        } catch (CipherException e) {
            logger.error("RawTransaction - private - bad credentials", e);
            throw e;
        }
    }

    private static class RawPrivateContract {
        String rawTransaction;
        int value;
        QuorumNode node;

        public RawPrivateContract(String rawTransaction, int value, QuorumNode node) {
            this.rawTransaction = rawTransaction;
            this.value = value;
            this.node = node;
        }
    }

    public String sign(final RawTransaction rawTransaction, Credentials credentials) {

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

            signedMessage = setPrivate(signedMessage);

        return Numeric.toHexString(signedMessage);
    }

    private byte[] setPrivate(final byte[] signedMessage) {
        // If the byte array RLP decodes to a list of size >= 1 containing a list of size >= 3
        // then find the 3rd element from the last. If the element is a RlpString of size 1 then
        // it should be the V component from the SignatureData structure -> mark the transaction as private.
        // If any of of the above checks fails then return the original byte array.
        var rlpWrappingList = RlpDecoder.decode(signedMessage);
        var result = signedMessage;
        if (!rlpWrappingList.getValues().isEmpty()) {
            var rlpList = rlpWrappingList.getValues().get(0);
            if (rlpList instanceof RlpList) {
                var rlpListSize = ((RlpList) rlpList).getValues().size();
                if (rlpListSize > 3) {
                    var vField = ((RlpList) rlpList).getValues().get(rlpListSize - 3);
                    if (vField instanceof RlpString) {
                        if (1 == ((RlpString) vField).getBytes().length) {
                            var first = ((RlpString) vField).getBytes()[0];

                            if (first == 28) {
                                ((RlpString) vField).getBytes()[0] = 38;
                            } else {
                                ((RlpString) vField).getBytes()[0] = 37;
                            }
                            result = RlpEncoder.encode(rlpList);
                        }
                    }
                }
            }
        }
        return result;
    }
}
