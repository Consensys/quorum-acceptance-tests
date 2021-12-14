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

package com.quorum.gauge.ext;

import com.quorum.gauge.common.PrivacyFlag;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.tx.ClientTransactionManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static com.quorum.gauge.services.AbstractService.DEFAULT_MAX_RETRY;
import static com.quorum.gauge.services.AbstractService.DEFAULT_SLEEP_DURATION_IN_MILLIS;

public class PrivateClientTransactionManager extends ClientTransactionManager {

    private List<PrivacyFlag> contractFlag;

    private List<String> mandatoryFor;

    private Quorum quorum;

    private Web3jService web3j;

    public PrivateClientTransactionManager(
        Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, List<PrivacyFlag> contractFlag, int attempts, int sleepDuration) {
        super(quorum, fromAddress, privateFrom, privateFor, attempts, sleepDuration);
        this.quorum = quorum;
        this.contractFlag = contractFlag;

        try {
            JsonRpc2_0Web3j q = (JsonRpc2_0Web3j) quorum;
            Field f = JsonRpc2_0Web3j.class.getDeclaredField("web3jService"); //NoSuchFieldException
            f.setAccessible(true);
            this.web3j = (Web3jService) f.get(q); //IllegalAccessException
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PrivateClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, List<String> mandatoryFor, List<PrivacyFlag> contractFlag) {
        this(quorum, fromAddress, privateFrom, privateFor, contractFlag, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
        this.mandatoryFor = mandatoryFor;
    }

    public PrivateClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, List<PrivacyFlag> contractFlag) {
        this(quorum, fromAddress, privateFrom, privateFor, contractFlag, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public PrivateClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor) {
        this(quorum, fromAddress, privateFrom, privateFor, List.of(PrivacyFlag.StandardPrivate), DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        PrivateTransaction tx;
        if (contractFlag != null) {
            if (Objects.nonNull(mandatoryFor)) {
                tx = new EnhancedPrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor(), mandatoryFor, contractFlag);
            } else {
                tx = new EnhancedPrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor(), contractFlag);
            }
        } else {
            tx = new PrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor());
        }
        return quorum.ethSendTransaction(tx).send();
    }

    @Override
    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException, TransactionException {
        final TransactionReceipt receipt = super.executeTransaction(gasPrice, gasLimit, to, data, value);

        return QuorumTransactionManagerService.maybeGetPrivateTransactionReceipt(web3j, receipt)
            .flatMap(EthGetTransactionReceipt::getTransactionReceipt)
            .orElse(receipt);
    }

    @Override
    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException, TransactionException {
        final TransactionReceipt receipt = super.executeTransaction(gasPrice, gasLimit, to, data, value, constructor);

        return QuorumTransactionManagerService.maybeGetPrivateTransactionReceipt(web3j, receipt)
            .flatMap(EthGetTransactionReceipt::getTransactionReceipt)
            .orElse(receipt);
    }


}
