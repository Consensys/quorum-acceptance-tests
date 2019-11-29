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
import com.thoughtworks.gauge.Gauge;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class ValueTransferPublicTransaction extends AbstractSpecImplementation {

    @Step("Send <value> Wei from a default account in <from> to a default account in <to> in a public transaction")
    public void sendTransaction(int value, QuorumNode from, QuorumNode to) {
        // backup the current balance
        String txHash = Observable.zip(
                accountService.getDefaultAccountBalance(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountBalance(to).subscribeOn(Schedulers.io()),
                (fromBalance, toBalance) -> {
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", from), fromBalance.getBalance());
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", to), toBalance.getBalance());
                    return true;
                })
                .flatMap( r -> transactionService.sendPublicTransaction(value, from, to))
                .blockingFirst().getTransactionHash();
        Gauge.writeMessage("Transaction Hash: %s", txHash);
        DataStoreFactory.getScenarioDataStore().put("tx_hash", txHash);
    }

    @Step("Transaction is accepted in the blockchain")
    public void verifyTransactionHash() {
        String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "tx_hash", String.class);
        // if the transaction is accepted, its receipt must be available in any node
        Predicate<? super EthGetTransactionReceipt> isReceiptPresent
            = ethGetTransactionReceipt -> ethGetTransactionReceipt.getTransactionReceipt().isPresent();

        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(QuorumNode.Node1, txHash)
                .repeatWhen(completed -> completed.delay(2, TimeUnit.SECONDS))
                .takeUntil(isReceiptPresent)
                .timeout(10, TimeUnit.SECONDS)
                .blockingLast().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("In <node>, the default account's balance is now less than its previous balance")
    public void verifyLesserBalance(QuorumNode node) {
        BigInteger prevBalance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), String.format("%s_balance", node), BigInteger.class);
        BigInteger actualBalance = accountService.getDefaultAccountBalance(node).blockingFirst().getBalance();

        assertThat(actualBalance).isLessThan(prevBalance);
    }

    @Step("In <node>, the default account's balance is now greater than its previous balance")
    public void verifyMoreBalance(QuorumNode node) {
        BigInteger prevBalance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), String.format("%s_balance", node), BigInteger.class);
        BigInteger actualBalance = accountService.getDefaultAccountBalance(node).blockingFirst().getBalance();

        assertThat(actualBalance).isGreaterThan(prevBalance);
    }

    @Step("Send <value> Wei from a default account in <from> to a default account in <to> in a signed public transaction")
    public void sendSignedTransaction(int value, QuorumNode from, QuorumNode to) {
        // backup the current balance
        String txHash = Observable.zip(
                accountService.getDefaultAccountBalance(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountBalance(to).subscribeOn(Schedulers.io()),
                (fromBalance, toBalance) -> {
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", from), fromBalance.getBalance());
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", to), toBalance.getBalance());
                    return true;
                })
                .flatMap(r -> transactionService.sendSignedPublicTransaction(value, from, to))
                .blockingFirst().getTransactionHash();

        DataStoreFactory.getScenarioDataStore().put("tx_hash", txHash);
    }
}
