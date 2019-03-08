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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.UnixDomainSocketFactory;
import org.web3j.quorum.enclave.Constellation;
import org.web3j.quorum.enclave.Enclave;
import org.web3j.quorum.enclave.Tessera;
import org.web3j.quorum.enclave.protocol.EnclaveService;
import org.web3j.quorum.tx.QuorumTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RawContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(RawContractService.class);

    @Autowired
    PrivacyService privacyService;

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
