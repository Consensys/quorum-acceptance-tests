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

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.quorum.gauge.ext.filltx.PrivateFillTransaction;
import com.quorum.gauge.sol.SimpleStorage;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.functions.Predicate;
import org.assertj.core.api.AssertionsForClassTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class FillTransaction extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    @Step("Deploy Simple Storage contract using fillTransaction api with an initial value of <initValue> called from <from> private for <to>. Name this contract as <contractName>")
    public void sendFillTransaction(int initValue, QuorumNode from, QuorumNode to, String contractName) {
        com.quorum.gauge.ext.filltx.FillTransaction filledTx = rawContractService.fillTransaction(from, to, initValue).blockingFirst().getResponseObject();
        if (filledTx == null) {
            // possible that tessera is older version which is leading to test case failure. skip
            // further filltx tests.
            DataStoreFactory.getSpecDataStore().put("skipFillTXTests", true);
        } else {
            assertThat(!filledTx.getRaw().isEmpty());
            DataStoreFactory.getSpecDataStore().put("skipFillTXTests", false);

            PrivateFillTransaction tx = filledTx.getPrivateTransaction(accountService.getDefaultAccountAddress(from).blockingFirst(), Arrays.asList(privacyService.id(to)));

            com.quorum.gauge.ext.filltx.FillTransaction fTx = rawContractService.signTransaction(from, tx).blockingFirst().getResponseObject();
            assertThat(!fTx.getRaw().isEmpty());

            String txHash = rawContractService.sendRawPrivateTransaction(from, fTx.getRaw(), to).blockingFirst().getTransactionHash();
            assertThat(!txHash.isEmpty());

            // if the transaction is accepted, its receipt must be available in any node
            Predicate<? super EthGetTransactionReceipt> isReceiptPresent
                = ethGetTransactionReceipt -> ethGetTransactionReceipt.getTransactionReceipt().isPresent();

            Optional<TransactionReceipt> receipt = transactionService
                .pollTransactionReceipt(networkProperty.getNode(QuorumNode.Node1.name()), txHash);

//            Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(QuorumNode.Node1, txHash)
//                .repeatWhen(completed -> completed.delay(2, TimeUnit.SECONDS))
//                .takeUntil(isReceiptPresent)
//                .timeout(10, TimeUnit.SECONDS)
//                .blockingLast().getTransactionReceipt();

            assertThat(receipt.isPresent()).isTrue();
            assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
            assertThat(!receipt.get().getContractAddress().isEmpty());

            Contract c = loadSimpleStorageContract(receipt.get().getContractAddress(), receipt.get());
            DataStoreFactory.getSpecDataStore().put(contractName, c);
        }

    }

    private SimpleStorage loadSimpleStorageContract(String contractAddress, TransactionReceipt transactionReceipt) {
        SimpleStorage c = SimpleStorage.load(
            contractAddress,
            null,
            (TransactionManager) null,
            null,
            null
        );
        c.setTransactionReceipt(transactionReceipt);

        return c;
    }

    @Step("If fillTransaction is successful, verify that <contractKey>'s payload is retrievable from <node>")
    public void verifyContractDeploy(String contractKey, QuorumNode node) {
        boolean skipTest = mustHaveValue(DataStoreFactory.getSpecDataStore(), "skipFillTXTests", java.lang.Boolean.class);

        if (!skipTest) {
            logger.debug("executing test to verify that payload is retrievable for {} from node {}", contractKey, node);
            Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractKey, Contract.class);
            EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).blockingFirst();

            AssertionsForClassTypes.assertThat(payload.getResult()).isNotEqualTo("0x");
        } else {
            logger.debug("skipping test to verify that payload is retrievable for {} from node {}", contractKey, node);
        }
    }

    @Step("If fillTransaction is successful, verify that <contractKey>'s payload is not retrievable from <node>")
    public void verifyContractNonDeploy(String contractKey, QuorumNode node) {
        boolean skipTest = mustHaveValue(DataStoreFactory.getSpecDataStore(), "skipFillTXTests", java.lang.Boolean.class);
        if (!skipTest) {
            Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractKey, Contract.class);
            EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).blockingFirst();

            AssertionsForClassTypes.assertThat(payload.getResult()).isEqualTo("0x");
        } else {
            logger.debug("skipping test to verify that payload is not retrievable for {} from node {}", contractKey, node);
        }

    }

    @Step("If fillTransaction is successful, verify that <contractKey>'s `get()` function execution in <node> returns <expectedValue>")
    public void verifyStorageValueForParticipant(String contractKey, QuorumNode node, int expectedValue) {
        boolean skipTest = mustHaveValue(DataStoreFactory.getSpecDataStore(), "skipFillTXTests", java.lang.Boolean.class);
        if (!skipTest) {
            Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractKey, Contract.class);
            int actualValue = contractService.readSimpleContractValue(node, c.getContractAddress());

            AssertionsForClassTypes.assertThat(actualValue).isEqualTo(expectedValue);

        } else {
            logger.debug("skipping test to verify storage value for {} from node {}", contractKey, node);
        }
    }

}
