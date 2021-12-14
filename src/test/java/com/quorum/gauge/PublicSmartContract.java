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

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PublicSmartContract extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(PublicSmartContract.class);

    @Step("Deploy `ClientReceipt` smart contract from a default account in <node>, named this contract as <contractName>")
    public void deployClientReceiptSmartContract(QuorumNetworkProperty.Node node, String contractName) {
        Contract c = contractService.createClientReceiptSmartContract(node).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName, c);
    }

    @Step("Deploy a simple smart contract with initial value <initialValue> in <source>'s default account, named this contract as <contractName>")
    public void setupContract(int initialValue, QuorumNode source, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up contract from {}", source);
        Contract contract = contractService.createSimpleContract(initialValue, source, null).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a simple smart contract from <source>, verify it does not get mined")
    public void setupContractFailsToBeMined(QuorumNode source) {
        logger.debug("Setting up contract from {}", source);
        String txFailedMsg = "Transaction receipt was not generated after";
        boolean failed = false;
        try {
            contractService.createSimpleContract(5, source, null).blockingFirst();
        } catch (Exception ex) {
            logger.debug("exception while deploying contract:{} msg:{}", ex.getMessage(), txFailedMsg);
            if (ex.getMessage().indexOf(txFailedMsg) >= 0) {
                failed = true;
            }
        }
        assertThat(failed).isTrue();
    }

    @Step("Deploy a simple smart contract from <source>, verify it gets mined")
    public void setupContractGetsMined(QuorumNode source) {
        logger.debug("Setting up contract from {}", source);
        String txFailedMsg = "Transaction receipt was not generated after";
        boolean failed = false;
        Contract c = null;
        try {
            c = contractService.createSimpleContract(5, source, null).blockingFirst();
        } catch (Exception ex) {
            logger.debug("exception while deploying contract:{} msg:{}", ex.getMessage(), txFailedMsg);
            if (ex.getMessage().indexOf(txFailedMsg) >= 0) {
                failed = true;
            }
        }
        assertThat(failed).isFalse();
        assertThat(c).isNotNull();
        assertThat(c.getContractAddress()).isNotNull();
    }

    @Step("<contractName> is mined")
    public void verifyContractIsMined(String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        assertThat(c.getTransactionReceipt().isPresent()).isTrue();
        assertThat(c.getTransactionReceipt().get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Execute <contractName>'s `deposit()` function <count> times with arbitrary id and value from <node>")
    public void excuteDesposit(String contractName, int count, QuorumNetworkProperty.Node node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        List<Observable<TransactionReceipt>> observables = new ArrayList<>();
        Scheduler scheduler = threadLocalDelegateScheduler(count);
        for (int i = 0; i < count; i++) {
            observables.add(contractService.updateClientReceipt(node, c.getContractAddress(), BigInteger.TEN).subscribeOn(scheduler));
        }
        List<TransactionReceipt> receipts = Observable.zip(observables, objects -> Observable.fromArray(objects).map(o -> (TransactionReceipt) o).toList().blockingGet()).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("receipts", receipts);
    }

    @Step("<node> has received <expectedTxCount> transactions which contain <expectedEventCount> log events in total")
    public void verifyLogEvents(QuorumNode node, int expectedTxCount, int expectedEventCount) {
        List<TransactionReceipt> originalReceipts = (List<TransactionReceipt>) DataStoreFactory.getScenarioDataStore().get("receipts");

        List<Observable<TransactionReceipt>> receiptsInNode = new ArrayList<>();
        Scheduler scheduler = threadLocalDelegateScheduler(expectedTxCount);
        for (TransactionReceipt r : originalReceipts) {
            receiptsInNode.add(transactionService.getTransactionReceipt(node, r.getTransactionHash())
                .map(tr -> {
                    if (tr.getTransactionReceipt().isPresent()) {
                        return tr.getTransactionReceipt().get();
                    } else {
                        throw new RuntimeException("retry");
                    }
                })
                .retryWhen(new RetryWithDelay(20, 3000))
                .subscribeOn(scheduler));
        }

        AtomicInteger actualTxCount = new AtomicInteger();
        AtomicInteger actualEventCount = new AtomicInteger();
        Observable.zip(receiptsInNode, args -> {
            for (Object o : args) {
                TransactionReceipt r = (TransactionReceipt) o;
                assertThat(r.isStatusOK()).isTrue();
                assertThat(r.getBlockNumber()).isNotEqualTo(BigInteger.ZERO);
                actualTxCount.getAndIncrement();
                actualEventCount.addAndGet(r.getLogs().size());
            }
            // Observable.zip complains when it receives a null result,
            // but we dont care about the real result so return a dummy instead
            return new Object();
        }).blockingFirst();

        assertThat(actualTxCount.get()).as("Transaction Count").isEqualTo(expectedTxCount);
        assertThat(actualEventCount.get()).as("Log Event Count").isEqualTo(expectedEventCount);
    }

    @Step("Wait for block height is multiple of <count> by sending arbitrary public transactions")
    public void waitForBlockHeightBySendingPublicTransaction(int count) {
        int bloomConfirmations = 256; // this value comes from bloomConfirms which is the number of confirmation blocks before a bloom section is moved
        int delta = 20; // marginal tollerance
        BigInteger currentBlockHeight = currentBlockNumber();
        int targetBlockHeight = currentBlockHeight.intValue() + (count - currentBlockHeight.intValue() % count) + bloomConfirmations + delta;
        List<Observable<? extends Contract>> contractObservables = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            contractObservables.add(contractService.createClientReceiptSmartContract(networkProperty.getNodes().values().stream().findFirst().get()).subscribeOn(Schedulers.io()));
        }
        while (currentBlockHeight.intValue() < targetBlockHeight) {
            // as this test will take time to complete so this log is important
            // to tell Travis not to kill the CI
            logger.warn("[Travis] Current block height = {}, targetBlockHeight = {}", currentBlockHeight.intValue(), targetBlockHeight);
            currentBlockHeight = Observable.zip(contractObservables, args -> args.length)
                .flatMap(i -> utilService.getCurrentBlockNumber())
                .blockingFirst()
                .getBlockNumber();
        }
    }

    @Step("Wait for block height is multiple of <count> by sending arbitrary public transactions on <node>")
    public void waitForBlockHeightBySendingPublicTransactionOnNode(int count, QuorumNetworkProperty.Node node) {
        int bloomConfirmations = 256; // this value comes from bloomConfirms which is the number of confirmation blocks before a bloom section is moved
        int delta = 20; // marginal tollerance
        BigInteger currentBlockHeight = currentBlockNumber();
        int targetBlockHeight = currentBlockHeight.intValue() + (count - currentBlockHeight.intValue() % count) + bloomConfirmations + delta;
        List<Observable<? extends Contract>> contractObservables = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            contractObservables.add(contractService.createClientReceiptSmartContract(node).subscribeOn(Schedulers.io()));
        }
        while (currentBlockHeight.intValue() < targetBlockHeight) {
            // as this test will take time to complete so this log is important
            // to tell Travis not to kill the CI
            logger.warn("[Travis] Current block height = {}, targetBlockHeight = {}", currentBlockHeight.intValue(), targetBlockHeight);
            currentBlockHeight = Observable.zip(contractObservables, args -> args.length)
                .flatMap(i -> utilService.getCurrentBlockNumber())
                .blockingFirst()
                .getBlockNumber();
        }
    }
}
