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
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.sol.SimpleStorage;
import io.reactivex.Observable;
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
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
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
import org.web3j.quorum.tx.QuorumTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public Observable<? extends Contract> createRawSimplePublicContract(int initialValue, WalletData wallet, QuorumNode source) {
        Web3j web3j = connectionFactory().getWeb3jConnection(source);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            RawTransactionManager qrtxm = new RawTransactionManager(
                    web3j,
                    credentials,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

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

    public Observable<TransactionReceipt> updateRawSimplePublicContract(QuorumNode source, WalletData wallet, String contractAddress, int newValue) {
        Web3j web3j = connectionFactory().getWeb3jConnection(source);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            RawTransactionManager qrtxm = new RawTransactionManager(
                    web3j,
                    credentials,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

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


    public Observable<? extends Contract> createRawSimplePrivateContract(int initialValue, WalletData wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            QuorumTransactionManager qrtxm = new QuorumTransactionManager(client,
                    credentials,
                    privacyService.id(source),
                    Arrays.asList(privacyService.id(target)),
                    enclave,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

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

    public Observable<TransactionReceipt> updateRawSimplePrivateContract(int newValue, String contractAddress, WalletData wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            Credentials credentials = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath());

            QuorumTransactionManager qrtxm = new QuorumTransactionManager(client,
                    credentials,
                    privacyService.id(source),
                    Arrays.asList(privacyService.id(target)),
                    enclave,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

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

    private Enclave buildEnclave(QuorumNode source, Quorum client){
        String thirdPartyURL = privacyService.thirdPartyUrl(source);
        if (thirdPartyURL.endsWith("ipc")){
            EnclaveService enclaveService = new EnclaveService("http://localhost", 12345, getIPCHttpClient(thirdPartyURL));
            return new Constellation(enclaveService, client);
        } else {
            URI uri = URI.create(thirdPartyURL);
            String enclaveUrl = uri.getScheme() + "://" + uri.getHost();
            EnclaveService enclaveService = new EnclaveService(enclaveUrl, uri.getPort(), httpClient);
            return new Tessera(enclaveService, client);
        }
    }

    private Map<String,OkHttpClient> ipcClients = new ConcurrentHashMap<>();

    private OkHttpClient getIPCHttpClient(String thirdPartyURL) {
        if (ipcClients.containsKey(thirdPartyURL)){
            return  ipcClients.get(thirdPartyURL);
        }
        OkHttpClient client = new OkHttpClient.Builder()
                                .socketFactory(new UnixDomainSocketFactory(new File(thirdPartyURL)))
                                .build();

        ipcClients.put(thirdPartyURL, client);

        return client;
    }

}
