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
package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Service
public class PrivateStateValidation extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(PrivateStateValidation.class);

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deploySimpleContract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Deploy a <flag> contract `C1` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC1Contract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Deploy a <flag> contract `C2` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC2Contract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Fail to execute <contractName>'s `get()` function in <node>")
    public void failGetExecution(String contractName, QuorumNode node) {
    }

    @Step("Fail to execute <contractName>'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void failSetExecution(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> nestedContractService.updateC1Contract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        assertThatThrownBy(
            () -> Observable.zip(receipts, (args) -> null).toBlocking().first()
        ).as("Expected exception thrown")
            .isNotNull();
    }

    @Step("Fire and forget execution of <contractName>'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForget(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> nestedContractService.updateC1Contract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        Observable.zip(receipts, (args) -> {
            Arrays.stream(args)
                .map(o -> (TransactionReceipt) o)
                .forEach(tr -> logger.debug(tr.getTransactionHash()));
            return null;
        }).doOnError(e -> logger.debug("Got exception but will ignore. Exception is {}", e.getMessage())).toBlocking().first();
    }
}
