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
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
