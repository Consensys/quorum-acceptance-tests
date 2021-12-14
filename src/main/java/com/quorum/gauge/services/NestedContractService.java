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
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.ext.EthStorageRoot;
import com.quorum.gauge.sol.C1;
import com.quorum.gauge.sol.C2;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NestedContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(NestedContractService.class);

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createC1Contract(int initialValue, QuorumNode source, QuorumNode target) {
        return createC1Contract(initialValue, source, Arrays.asList(target), Arrays.asList(PrivacyFlag.StandardPrivate));
    }

    public Observable<? extends Contract> createC1Contract(int initialValue, QuorumNode source, List<QuorumNode> target, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C1.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createC1ContractWithMandatoryRecipients(int initialValue, QuorumNode source, List<QuorumNode> target, List<QuorumNode> mandatoryFor, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                mandatoryFor.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C1.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createPublicC1Contract(int initialValue, QuorumNode source) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = clientTransactionManager(
                client,
                address,
                null,
                null);
            return C1.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                BigInteger.valueOf(initialValue)).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createC2Contract(String c1Address, QuorumNode source, QuorumNode target) {
        return createC2Contract(c1Address, source, Arrays.asList(target), Arrays.asList(PrivacyFlag.StandardPrivate));
    }

    public Observable<? extends Contract> createC2Contract(String c1Address, QuorumNode source, List<QuorumNode> target, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C2.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                c1Address).flowable().toObservable();
        });
    }

    public Observable<? extends Contract> createC2ContractWithMandatoryRecipients(String c1Address, QuorumNode source, List<QuorumNode> target, List<QuorumNode> mandatoryFor, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                mandatoryFor.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C2.deploy(client,
                clientTransactionManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT,
                c1Address).flowable().toObservable();
        });
    }

    // Read-only contract
    public int readC1Value(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return C1.load(contractAddress, client, txManager,
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

    // Read-only contract
    public int readC2Value(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory().getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return C2.load(contractAddress, client, txManager,
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

    public Observable<TransactionReceipt> restoreFromC1(QuorumNode node, List<QuorumNode> target, String contractAddress, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(node);
        return accountService.getDefaultAccountAddress(node).flatMap(address -> {
            PrivateClientTransactionManager txManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C2.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).restoreFromC1().flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> updateC1Contract(QuorumNode source, List<QuorumNode> target, String contractAddress, int newValue) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager txManager = clientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()));
            return C1.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> updateC2Contract(QuorumNode source, List<QuorumNode> target, String contractAddress, int newValue, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            PrivateClientTransactionManager txManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C2.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).set(BigInteger.valueOf(newValue)).flowable().toObservable();
        });
    }

    public Observable<EthStorageRoot> getStorageRoot(QuorumNode node, String contractAddress) {
        Request<String, EthStorageRoot> request = new Request<>(
            "eth_storageRoot",
            Arrays.asList(contractAddress),
            connectionFactory().getWeb3jService(node),
            EthStorageRoot.class);
        return request.flowable().toObservable();
    }

    public Observable<TransactionReceipt> newContractC2(QuorumNode source, List<QuorumNode> target, String contractAddress, BigInteger newValue, List<PrivacyFlag> flags) {
        Quorum client = connectionFactory().getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            PrivateClientTransactionManager txManager = new PrivateClientTransactionManager(
                client,
                address,
                null,
                target.stream().map(n -> privacyService.id(n)).collect(Collectors.toList()),
                flags);
            return C1.load(contractAddress, client, txManager,
                BigInteger.valueOf(0),
                DEFAULT_GAS_LIMIT).newContractC2(newValue).flowable().toObservable();
        });
    }
}
