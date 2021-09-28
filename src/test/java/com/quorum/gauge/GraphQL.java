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
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class GraphQL extends AbstractSpecImplementation {

    private final static Logger LOGGER = LoggerFactory.getLogger(GraphQL.class);

    @Step("Get block number from <node> graphql and it should be greater than or equal to <snapshotName>")
    public void getCurrentBlockNumber(QuorumNode node, String snapshotName) {
        int currentBlockHeight = ((BigInteger) DataStoreFactory.getScenarioDataStore().get(snapshotName)).intValue();
        // When running istanbul it is possible that the snapshot block height increases between the calls to eth.blockHeight and
        // graphql block{number}. Update the test to verify that the graphql returned value (the latter call) is greater
        // than or equal to the value returned by eth.blockNumber.
        assertThat(graphQLService.getBlockNumber(node).blockingGet().intValue()).isGreaterThanOrEqualTo(currentBlockHeight);
    }

    @Step("Get isPrivate field for <contractName>'s contract deployment transaction using GraphQL query from <node> and it should equal to <expected>")
    public void GetIsPrivate(String contractName, QuorumNode node, Boolean expected) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract"));

        // if a transactionHash has been stored then wait until node has executed it
        // TODO this is a temporary fix for particularly flaky tests where state is being read before it has been
        // updated - we should probably rework all tests to check that the state is ready before getting
        Optional<String> transactionHash = Optional.ofNullable(DataStoreFactory.getScenarioDataStore().get("transactionHash"))
            .map(Object::toString);
        transactionHash.ifPresent(h -> transactionService.waitForTransactionReceipt(node, h));

        boolean got = graphQLService.getIsPrivate(node, receipt.getTransactionHash()).blockingGet();
        assertThat(got).isEqualTo(expected);
    }

    @Step("Get isPrivate field for <contractName>'s contract deployment internal transaction using GraphQL query from <node> and it should equal to <expectedStr>")
    public void GetInternalIsPrivate(String contractName, QuorumNode node, String expectedStr) {
        Boolean expected = "null".equals(expectedStr) ? null : Boolean.parseBoolean(expectedStr);

        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract"));

        Boolean got = graphQLService.getInternalIsPrivate(node, receipt.getTransactionHash())
            .blockingGet()
            .orElse(null);
        assertThat(got).isEqualTo(expected);
    }

    @Step("Get privateInputData field for <contractName>'s contract deployment transaction using GraphQL query from <node> and it should be the same as eth_getQuorumPayload")
    public void GetPrivateInputData(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, transactionHash).blockingFirst();
        assertThat(graphQLService.getPrivatePayload(node, transactionHash).blockingGet()).isEqualTo(payload.getResult());
    }

    @Step("Get privateInputData field for <contractName>'s contract deployment transaction using GraphQL query from <node> and it should be equal to <expected>")
    public void GetPrivateInputData(String contractName, QuorumNode node, String expected) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        assertThat(graphQLService.getPrivatePayload(node, transactionHash).blockingGet()).isEqualTo(expected);
    }

    @Step("Get privateInputData field for <contractName>'s contract deployment internal transaction using GraphQL query from <node> and it should be the same as eth_getQuorumPayload")
    public void GetInternalPrivateInputData(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        Transaction privateTx = privacyMarkerTransactionService.getPrivateTransaction(node, transactionHash)
            .blockingFirst()
            .getTransaction()
            .orElseThrow(() -> new RuntimeException("no internal transaction for transaction"));

        String expected = transactionService.getQuorumPayload(node, privateTx.getInput())
            .blockingFirst()
            .getPrivatePayload();

        String got = graphQLService.getInternalPrivatePayload(node, transactionHash)
            .blockingGet()
            .orElse(null);

        assertThat(got).isEqualTo(expected);
    }

    @Step("Get privateInputData field for <contractName>'s contract deployment internal transaction using GraphQL query from <node> and it should be equal to <expected>")
    public void GetInternalPrivateInputData(String contractName, QuorumNode node, String expected) {
        expected = "null".equals(expected) ? null : expected;

        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        String got = graphQLService.getInternalPrivatePayload(node, transactionHash)
            .blockingGet()
            .orElse(null);

        assertThat(got).isEqualTo(expected);
    }
}
