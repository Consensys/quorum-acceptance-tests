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
import com.quorum.gauge.common.Wallet;
import com.quorum.gauge.sol.SimpleStorage;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
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
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
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

    public Observable<? extends Contract> createRawSimplePublicContract(int initialValue, Wallet wallet, QuorumNode source) {
        Web3j web3j = connectionFactory().getWeb3jConnection(source);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            RawTransactionManager qrtxm = new RawTransactionManager(
                    web3j,
                    credentials,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

            return SimpleStorage.deploy(web3j,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT,
                    BigInteger.valueOf(initialValue)).observable();

        } catch (IOException e) {
            logger.error("RawTransaction- public", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - public - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<TransactionReceipt> updateRawSimplePublicContract(QuorumNode source, Wallet wallet, String contractAddress, int newValue) {
        Web3j web3j = connectionFactory().getWeb3jConnection(source);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            RawTransactionManager qrtxm = new RawTransactionManager(
                    web3j,
                    credentials,
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

            return SimpleStorage.load(contractAddress, web3j,
                    qrtxm,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).observable();

        } catch (IOException e) {
            logger.error("RawTransaction- public", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - public - bad credentials", e);
            return Observable.error(e);
        }
    }


    public Observable<? extends Contract> createRawSimplePrivateContract(int initialValue, Wallet wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

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
                    BigInteger.valueOf(initialValue)).observable();

        } catch (IOException e) {
            logger.error("RawTransaction - private", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - private - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<TransactionReceipt> updateRawSimplePrivateContract(int newValue, String contractAddress, Wallet wallet, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

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
                    DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).observable();

        } catch (IOException e) {
            logger.error("RawTransaction - private", e);
            return Observable.error(e);
        } catch (CipherException e) {
            logger.error("RawTransaction - private - bad credentials", e);
            return Observable.error(e);
        }
    }

    public Observable<EthSendTransaction> createRawSimplePrivateContractUsingEthApi(int initialValue, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        String payload = base64SimpleStorageConstructorBytecode(initialValue);
        SendResponse storeRawResponse = enclave.storeRawRequest(
            payload,
            privacyService.id(source),
            Collections.emptyList()
        );

        String tmHash = base64ToHex(storeRawResponse.getKey());
        return transactionService.sendSignedPrivateTransaction(tmHash, source, target, null);
    }

    public Observable<EthGetTransactionReceipt> updateRawSimplePrivateContractUsingEthApi(int newValue, String contractAddress, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory().getConnection(source);
        Enclave enclave = buildEnclave(source, client);

        String payload = base64SimpleStorageSetBytecode(newValue);
        SendResponse storeRawResponse = enclave.storeRawRequest(
            payload,
            privacyService.id(source),
            Collections.emptyList()
        );

        String tmHash = base64ToHex(storeRawResponse.getKey());
        EthSendTransaction ethSendTransaction = transactionService.sendSignedPrivateTransaction(tmHash, source, target, contractAddress).toBlocking().first();

        logger.debug("sent tx: {}", ethSendTransaction.getTransactionHash());

        return transactionService.getTransactionReceipt(source, ethSendTransaction.getTransactionHash())
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000));
    }

    private String base64SimpleStorageConstructorBytecode(int initialValue) {
        String hexBytecode = "0x6060604052341561000f57600080fd5b604051602080610149833981016040528080519060200190919050505b806000819055505b505b610104806100456000396000f30060606040526000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632a1afcd914605157806360fe47b11460775780636d4ce63c146097575b600080fd5b3415605b57600080fd5b606160bd565b6040518082815260200191505060405180910390f35b3415608157600080fd5b6095600480803590602001909190505060c3565b005b341560a157600080fd5b60a760ce565b6040518082815260200191505060405180910390f35b60005481565b806000819055505b50565b6000805490505b905600a165627a7a72305820d5851baab720bba574474de3d09dbeaabc674a15f4dd93b974908476542c23f00029";

        return base64BytecodeWithArg(hexBytecode, initialValue);
    }

    private String base64SimpleStorageSetBytecode(int newValue) {
        String methodId = "0x60fe47b1"; // method hash for simple storage set
        return base64BytecodeWithArg(methodId, newValue);
    }

    private String base64BytecodeWithArg(String hexBytecode, int arg) {
        byte[] bytecode = Numeric.hexStringToByteArray(hexBytecode);
        // arg padded to a 32-byte array as defined in ABI spec https://solidity.readthedocs.io/en/develop/abi-spec.html#examples
        byte[] argBytes = Numeric.toBytesPadded(BigInteger.valueOf(arg), 32);

        byte[] bytecodeAndArg = new byte[bytecode.length + argBytes.length];
        System.arraycopy(bytecode, 0, bytecodeAndArg, 0, bytecode.length);
        System.arraycopy(argBytes, 0, bytecodeAndArg, bytecode.length, argBytes.length);

        return Base64.getEncoder().encodeToString(bytecodeAndArg);
    }

    private String base64ToHex(String b64) {
        byte[] raw = Base64.getDecoder().decode(b64);
        return Numeric.toHexString(raw);
    }

    private Enclave buildEnclave(QuorumNode source, Quorum client){
        String thirdPartyURL = privacyService.thirdPartyUrl(source);
        if (thirdPartyURL.endsWith("ipc")){
            EnclaveService enclaveService = new EnclaveService("http://localhost", 12345, getIPCHttpClient(thirdPartyURL));
            Enclave enclave = new Constellation(enclaveService, client);
            return enclave;
        } else {
            URI uri = URI.create(thirdPartyURL);
            String enclaveUrl = uri.getScheme() + "://" + uri.getHost();
            EnclaveService enclaveService = new EnclaveService(enclaveUrl, uri.getPort(), httpClient);
            Enclave enclave = new Tessera(enclaveService, client);
            return enclave;
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
