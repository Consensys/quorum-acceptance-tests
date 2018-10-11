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
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PublicSmartContract extends AbstractSpecImplementation {

    @Step("Deploy `ClientReceipt` smart contract from a default account in <node>, named this contract as <contractName>")
    public void deployClientReceiptSmartContract(QuorumNode node, String contractName) {
        Contract c = contractService.createClientReceiptSmartContract(node).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName, c);
    }

    @Step("<contractName> is mined")
    public void verifyContractIsMined(String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        assertThat(c.getTransactionReceipt().isPresent()).isTrue();
        assertThat(c.getTransactionReceipt().get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Execute <contractName>'s `deposit()` function <count> times with arbitrary id and value from <node>")
    public void excuteDesposit(String contractName, int count, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        List<Observable<TransactionReceipt>> observables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            observables.add(contractService.updateClientReceipt(node, c.getContractAddress(), BigInteger.TEN).subscribeOn(Schedulers.io()));
        }
        List<TransactionReceipt> receipts = Observable.zip(observables, objects -> Observable.from(objects).map(o -> (TransactionReceipt)o ).toList().toBlocking().first()).toBlocking().first();

        DataStoreFactory.getScenarioDataStore().put("receipts", receipts);
    }

    @Step("<node> has received <expectedTxCount> transactions which contain <expectedEventCount> log events in total")
    public void verifyLogEvents(QuorumNode node, int expectedTxCount, int expectedEventCount) {
        List<TransactionReceipt> originalReceipts = (List<TransactionReceipt>) DataStoreFactory.getScenarioDataStore().get("receipts");

        List<Observable<TransactionReceipt>> receiptsInNode = new ArrayList<>();
        for (TransactionReceipt r : originalReceipts) {
            receiptsInNode.add(transactionService.getTransactionReceipt(node, r.getTransactionHash()).map(tr -> tr.getTransactionReceipt().get()).subscribeOn(Schedulers.io()));
        }

        AtomicInteger actualTxCount = new AtomicInteger();
        AtomicInteger actualEventCount = new AtomicInteger();
        Observable.zip(receiptsInNode, (FuncN<Void>) args -> {
            for (Object o : args) {
                TransactionReceipt r = (TransactionReceipt) o;
                assertThat(r.isStatusOK()).isTrue();
                assertThat(r.getBlockNumber()).isNotEqualTo(BigInteger.ZERO);
                actualTxCount.getAndIncrement();
                actualEventCount.addAndGet(r.getLogs().size());
            }
            return null;
        }).toBlocking().first();

        assertThat(actualTxCount.get()).as("Transaction Count").isEqualTo(expectedTxCount);
        assertThat(actualEventCount.get()).as("Log Event Count").isEqualTo(expectedEventCount);
    }

}
