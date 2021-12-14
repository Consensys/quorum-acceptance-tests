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
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.sol.IncreasingSimpleStorage;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
public class IncreasingSimpleStorageContractService extends AbstractService {

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;


    public Observable<? extends Contract> createIncreasingSimpleStorageContract(int initialValue, String source, List<String> privateForNodes) {
        Quorum client = connectionFactory().getConnection(networkProperty().getNode(source));
        return accountService.getAccountAddress(networkProperty().getNode(source), null).flatMap(address -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                privateForNodes != null ? privateForNodes.stream().map(QuorumNode::valueOf).map(privacyService::id).collect(Collectors.toList()) : null,
                emptyList());
            return IncreasingSimpleStorage.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> setValue(String contractAddress, int value, String source, List<String> privateForNodes) {
        final Quorum client = connectionFactory().getConnection(QuorumNode.valueOf(source));
        return accountService.getDefaultAccountAddress(networkProperty().getNode(source))
            .map(address -> new PrivateClientTransactionManager(client, address, null, privateForNodes != null ? privateForNodes.stream().map(QuorumNode::valueOf).map(privacyService::id).collect(Collectors.toList()) : null, Collections.emptyList()))
            .flatMap(txManager -> Observable.fromFuture(IncreasingSimpleStorage.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(value)).sendAsync())
            );
    }

    public Observable<BigInteger> getValue(String contractAddress, String source) {
        final Quorum client = connectionFactory().getConnection(QuorumNode.valueOf(source));
        return accountService.getDefaultAccountAddress(networkProperty().getNode(source))
            .map(address -> new PrivateClientTransactionManager(client, address, null, null, Collections.emptyList()))
            .flatMap(txManager -> IncreasingSimpleStorage.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).get().flowable().toObservable()
            );
    }

}
