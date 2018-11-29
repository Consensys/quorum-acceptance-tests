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
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.QuorumRawTransactionManager;
import rx.Observable;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

@Service
public class RawContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(RawContractService.class);

    @Autowired
    PrivacyService privacyService;

    @Autowired
    OkHttpClient httpClient;

    public Observable<? extends Contract> createRawSimplePublicContract(int initialValue, Wallet wallet, QuorumNode source) {
        Web3jService web3jService = connectionFactory().getWeb3jService(source);
        Web3j web3j = Web3j.build(web3jService);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            QuorumRawTransactionManager qrtxm = new QuorumRawTransactionManager(httpClient,
                    privacyService.thirdPartyUrl(source),
                    web3j,
                    web3jService,
                    credentials, null,
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

        Web3jService web3jService = connectionFactory().getWeb3jService(source);
        Web3j web3j = Web3j.build(web3jService);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            QuorumRawTransactionManager qrtxm = new QuorumRawTransactionManager(httpClient,
                    privacyService.thirdPartyUrl(source),
                    web3j,
                    web3jService,
                    credentials,
                    null,
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
        Web3jService web3jService = connectionFactory().getWeb3jService(source);
        Web3j web3j = Web3j.build(web3jService);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            QuorumRawTransactionManager qrtxm = new QuorumRawTransactionManager(httpClient,
                    privacyService.thirdPartyUrl(source),
                    web3j,
                    web3jService,
                    credentials,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

            return SimpleStorage.deploy(web3j,
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

        Web3jService web3jService = connectionFactory().getWeb3jService(source);
        Web3j web3j = Web3j.build(web3jService);

        try {
            QuorumNetworkProperty.WalletData walletData = privacyService.walletData(wallet);
            Credentials credentials = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());

            QuorumRawTransactionManager qrtxm = new QuorumRawTransactionManager(httpClient,
                    privacyService.thirdPartyUrl(source),
                    web3j,
                    web3jService,
                    credentials,
                    Arrays.asList(privacyService.id(target)),
                    DEFAULT_MAX_RETRY,
                    DEFAULT_SLEEP_DURATION_IN_MILLIS);

            return SimpleStorage.load(contractAddress, web3j,
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

}
