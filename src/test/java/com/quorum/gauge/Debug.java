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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.JsonResponse;
import com.quorum.gauge.ext.StringResponse;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class Debug extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(Debug.class);

    @Step("Trace transaction <transaction> on <node> name it <traceName>")
    public void traceTransaction(String transactionName, String node, String traceName) {
        String txHash = mustHaveValue(DataStoreFactory.getSpecDataStore(), transactionName, String.class);
        final Observable<JsonResponse> stringResponseObservable = debugService.traceTransaction(networkProperty.getNode(node), txHash);
        JsonNode result = stringResponseObservable.blockingFirst().getResult();
        assertThat(result).isNotNull();
        DataStoreFactory.getSpecDataStore().put(traceName, result);
        DataStoreFactory.getScenarioDataStore().put(traceName, result);
    }

    @Step("Retrieve private state root on <node> for <blockHeight> and name it <psrName>")
    public void privateStateRoot(String node, String blockHeight, String psrName) {
        final String blockNumber = "0x" + mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockHeight, BigInteger.class).toString(16);
        final Observable<StringResponse> stringResponseObservable = debugService.privateStateRoot(networkProperty.getNode(node), blockNumber);
        String result = stringResponseObservable.blockingFirst().getResult();
        DataStoreFactory.getSpecDataStore().put(psrName, result);
        DataStoreFactory.getScenarioDataStore().put(psrName, result);
    }

    @Step("Check on <node> that account <account> does not exist at <blockHeight>")
    public void accountDoesNotExistAtBlockHeight(String node, String account, String blockHeight) {
        final String blockNumber = "0x" + mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockHeight, BigInteger.class).toString(16);
        final Observable<JsonResponse> jsonResponse = debugService.dumpAddress(networkProperty.getNode(node), account, blockNumber);
        JsonResponse result = jsonResponse.blockingFirst();

        assertThat(result.getResult()).isNull();
        assertThat(result.hasError()).isTrue();
        assertThat(result.getError().getMessage()).contains("error retrieving state");
    }

    @Step("Retrieve the empty state root on <node> for <blockHeight> and name it <psrName>")
    public void emptyStateRoot(String node, String blockHeight, String psrName) {
        final String blockNumber = "0x" + mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockHeight, BigInteger.class).toString(16);
        final Observable<StringResponse> stringResponseObservable = debugService.defaultStateRoot(networkProperty.getNode(node), blockNumber);
        String result = stringResponseObservable.blockingFirst().getResult();
        DataStoreFactory.getSpecDataStore().put(psrName, result);
        DataStoreFactory.getScenarioDataStore().put(psrName, result);
    }

    @Step("Check that private state root <psr1> is equal to <psr2>")
    public void psrsAreEqual(String psr1, String psr2){
        String psr1Val = mustHaveValue(psr1, String.class);
        String psr2Val = mustHaveValue(psr2, String.class);
        assertThat(psr1Val).isEqualTo(psr2Val);
    }

    @Step("Check that private state root <psr1> is different from <psr2>")
    public void psrsAreDifferent(String psr1, String psr2){
        String psr1Val = mustHaveValue(psr1, String.class);
        String psr2Val = mustHaveValue(psr2, String.class);
        assertThat(psr1Val).isNotEqualTo(psr2Val);
    }


    @Step("Check that <trace1> is equal to <trace2>")
    public void tracesAreEqual(String trace1, String trace2){
        JsonNode jnTrace1 = mustHaveValue(trace1, JsonNode.class);
        JsonNode jnTrace2 = mustHaveValue(trace2, JsonNode.class);
        assertThat(jnTrace1).isEqualTo(jnTrace2);
    }

    @Step("Check that <trace1> is different from <trace2>")
    public void tracesAreDifferent(String trace1, String trace2){
        JsonNode jnTrace1 = mustHaveValue(trace1, JsonNode.class);
        JsonNode jnTrace2 = mustHaveValue(trace2, JsonNode.class);
        assertThat(jnTrace1).isNotEqualTo(jnTrace2);
    }

    @Step("Check that <trace> is empty")
    public void traceIsEmpty(String trace){
        ObjectNode jnTrace = mustHaveValue(trace, ObjectNode.class);
        assertThat(jnTrace.get("structLogs").isEmpty()).isTrue();
    }

    @Step("Check that <trace> is not empty")
    public void traceIsNotEmpty(String trace){
        ObjectNode jnTrace = mustHaveValue(trace, ObjectNode.class);
        assertThat(jnTrace.get("structLogs").size()).isGreaterThan(0);
    }
}
