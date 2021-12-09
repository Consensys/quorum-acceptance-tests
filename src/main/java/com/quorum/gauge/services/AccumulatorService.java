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
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.sol.Accumulator;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AccumulatorService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(AccumulatorService.class);

    private static final int POLLING_INTERVAL = 500;

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Accumulator> createAccumulatorPublicContract(QuorumNetworkProperty.Node source, BigInteger gas, int initVal) {
        Quorum client = connectionFactory().getConnection( source);

        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            org.web3j.tx.ClientTransactionManager clientTransactionManager = new org.web3j.tx.ClientTransactionManager(
                client,
                address,
                DEFAULT_MAX_RETRY,
                DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return Accumulator.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas, BigInteger.valueOf(initVal)).flowable().toObservable();
        });
    }

    public Observable<? extends Accumulator> createAccumulatorPrivateContract(QuorumNetworkProperty.Node source, List<QuorumNetworkProperty.Node> targets, BigInteger gas, int initVal, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        final List<String> privateFor;
        if (null != targets) {
            privateFor = targets.stream().filter(Objects::nonNull).map(q -> privacyService.id(q)).collect(Collectors.toList());
        } else {
            privateFor = null;
        }

        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                privateFor,
                flags,
                DEFAULT_MAX_RETRY,
                DEFAULT_SLEEP_DURATION_IN_MILLIS);
            return Accumulator.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                gas, BigInteger.valueOf(initVal)).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> incAccumulatorPrivate(final QuorumNetworkProperty.Node source,
                                                                final List<QuorumNetworkProperty.Node> target,
                                                                final String contractAddress,
                                                                final BigInteger gasLimit,
                                                                final int increment,
                                                                final List<PrivacyFlag> flags) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(increment);
        final List<String> privateFor = target.stream().map(q -> privacyService.id(q)).collect(Collectors.toList());

        return accountService.getDefaultAccountAddress(source).flatMap(acctAddress -> {
            PrivateClientTransactionManager txManager = new PrivateClientTransactionManager(
                client, acctAddress, null, privateFor, flags, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS
            );
            Accumulator accumulator = Accumulator.load(
                contractAddress, client, txManager, BigInteger.ZERO, gasLimit);
            return accumulator.inc(value).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> incAccumulatorPublic(final QuorumNetworkProperty.Node source,
                                                               final String contractAddress,
                                                               final BigInteger gasLimit,
                                                               final int increment) {
        final Quorum client = connectionFactory().getConnection(source);
        final BigInteger value = BigInteger.valueOf(increment);

        return accountService.getDefaultAccountAddress(source).flatMap(acctAddress -> {
            org.web3j.tx.ClientTransactionManager txManager = new org.web3j.tx.ClientTransactionManager(
                client,
                acctAddress,
                DEFAULT_MAX_RETRY,
                DEFAULT_SLEEP_DURATION_IN_MILLIS);
            Accumulator accumulator = Accumulator.load(
                contractAddress, client, txManager, BigInteger.ZERO, gasLimit);
            return accumulator.inc(value).flowable().toObservable();
        });
    }

    public int get(QuorumNetworkProperty.Node node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return Accumulator.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).get().send().intValue();
        } catch (ContractCallException cce) {
            if (cce.getMessage().contains("Empty value (0x)")) {
                return 0;
            }
            logger.error("accumulator.get()", cce);
            throw new RuntimeException(cce);
        } catch (Exception e) {
            logger.error("accumulator.get()", e);
            throw new RuntimeException(e);
        }
    }

    public Disposable subscribeTo(QuorumNetworkProperty.Node node, String contractAddress, List<Accumulator.IncEventEventResponse> list) {
        Quorum client = connectionFactory().getConnection(node, POLLING_INTERVAL);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            Accumulator acc =  Accumulator.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT);
            return acc.incEventEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(list::add);
        } catch (Exception e) {
            logger.error("accumulator.subscribe()", e);
            throw new RuntimeException(e);
        }
    }

    public void sleepForPollingInterval(){
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            throw new RuntimeException("sleeping interrupted");
        }
    }

}
