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
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class ValueTransferPrivateTransaction {
    private static final Logger logger = LoggerFactory.getLogger(ValueTransferPrivateTransaction.class);

    @Autowired
    TransactionService transactionService;

    @Step("Send some ETH from a default account in <from> to a default account in <to> in a private transaction.")
    public void sendTransaction(QuorumNode from, QuorumNode to) {
        Response.Error err = transactionService.sendPrivateTransaction(new Random().nextInt(10), from, to).toBlocking().first().getError();

        DataStoreFactory.getScenarioDataStore().put("error", err);
    }

    @Step("Send some ETH from a default account in <from> to a default account in <to> in a signed private transaction.")
    public void sendSignedTransaction(QuorumNode from, QuorumNode to) {
        Response.Error err = transactionService.sendSignedPrivateTransaction(new Random().nextInt(10), from, to).toBlocking().first().getError();

        DataStoreFactory.getScenarioDataStore().put("error", err);
    }

    @Step("Error message <expectedErrorMsg> is returned.\n")
    public void verifyError(String expectedErrorMsg) {
        Response.Error err = (Response.Error) DataStoreFactory.getScenarioDataStore().get("error");

        assertThat(err).as("An error must be returned").isNotNull();
        assertThat(err.getMessage()).isEqualTo(expectedErrorMsg);
    }
}
