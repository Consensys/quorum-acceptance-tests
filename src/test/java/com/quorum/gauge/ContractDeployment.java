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
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class ContractDeployment extends AbstractSpecImplementation {

    @Step("Transaction Hash is <status> returned for <contractName>")
    public void transactionHash(Status status, String contractName) {
        Contract c = mustHaveValue(contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElse(new TransactionReceipt()).getTransactionHash();

        switch (status) {
            case successfully:
                assertThat(transactionHash).isNotBlank();
                break;
            case unsuccessfully:
                assertThat(transactionHash).isBlank();
                break;
        }

        DataStoreFactory.getScenarioDataStore().put(contractName + "_transactionHash", transactionHash);
    }

    @Step("Transaction Receipt is <status> available in <nodeList> for <contractName>")
    public void transactionReceipt(Status status, String nodeList, String contractName) {
        String[] nodes = nodeList.split(",");
        String transactionHash = mustHaveValue(contractName + "_transactionHash", String.class);
        List<Observable<Optional<TransactionReceipt>>> receiptObsevables = new ArrayList<>();
        for (String nodeStr : nodes) {
            QuorumNetworkProperty.Node node = networkProperty.getNode(nodeStr);
            receiptObsevables.add(
                Observable.just(transactionService.pollTransactionReceipt(node, transactionHash))
            );
        }
        Observable.zip(receiptObsevables, receipts -> {
            for (int i = 0; i < receipts.length; i++) {
                Optional<TransactionReceipt> r = (Optional<TransactionReceipt>) receipts[i];
                switch (status) {
                    case successfully:
                        assertThat(r.isPresent() && r.get().isStatusOK()).as("Transaction Receipt in " + nodes[i]).isTrue();
                        assertThat(r.get().getBlockNumber()).as("Block Number in Transaction Receipt in " + nodes[i]).isNotEqualTo(currentBlockNumber());
                        break;
                    case unsuccessfully:
                        assertThat(!r.isPresent() || !r.get().isStatusOK()).as("Transaction Receipt in " + nodes[i]).isTrue();
                        break;
                }
            }
            return true;
        }).blockingFirst();
    }
}
