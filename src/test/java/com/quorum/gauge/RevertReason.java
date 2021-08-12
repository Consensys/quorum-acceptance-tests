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

import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.IncreasingSimpleStorageContractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.assertj.core.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.AbiTypes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class RevertReason extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(RevertReason.class);
    private static final String REVERT_REASON_METHOD_ID = "0x08c379a0"; // Numeric.toHexString(Hash.sha3("Error(string)".getBytes())).substring(0, 10)
    private static final List<TypeReference<Type>> REVERT_REASON_TYPES = Collections.singletonList(TypeReference.create((Class<Type>) AbiTypes.getType("string")));

    @Autowired
    private IncreasingSimpleStorageContractService increasingSimpleStorageContractService;

    private static List<String> parseNodes(String nodes) {
        List<String> privateForList = null;
        if (!Strings.isNullOrEmpty(nodes)) {
            privateForList = Arrays.stream(nodes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        }
        return privateForList;
    }

    @Step("Deploy IncreasingSimpleStorage contract with initial value <value> from <node>")
    public void deployIncreasingSimpleStorage(int value, String node) {
        this.deployIncreasingSimpleStorage(value, node, null);
    }

    @Step("Deploy private IncreasingSimpleStorage contract with initial value <value> from <node> and private for <nodes>")
    public void deployIncreasingSimpleStorage(int value, String node, String nodes) {
        Contract contract = increasingSimpleStorageContractService.createIncreasingSimpleStorageContract(value, node, parseNodes(nodes)).blockingFirst();
        DataStoreFactory.getScenarioDataStore().put("contractAddress", contract.getContractAddress());
        DataStoreFactory.getScenarioDataStore().put("transactionHash", contract.getTransactionReceipt().get().getTransactionHash());
    }

    @Step("Set value <value> from <node>")
    public void setValueOnContract(int value, String node) throws TransactionException {
        this.setValueOnContract(value, node, null);
    }

    @Step("Set value <value> from <node> and private for <nodes>")
    public void setValueOnContract(int value, String node, String nodes) throws TransactionException {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();
        TransactionReceipt transactionReceipt = increasingSimpleStorageContractService.setValue(contractAddress, value, node, parseNodes(nodes)).blockingFirst();
        DataStoreFactory.getScenarioDataStore().put("transactionHash", transactionReceipt.getTransactionHash());
    }

    @Step("Set value <value> from <node> fails with reason <reasonText>")
    public void setValueOnContractFailsWithReason(int value, String node, String reasonText) {
        this.setValueOnContractFailsWithReason(value, node, null, reasonText);
    }

    @Step("Set value <value> from <node> and private for <nodes> fails with reason <reasonText>")
    public void setValueOnContractFailsWithReason(int value, String node, String nodes, String reasonText) {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();
        try {
            increasingSimpleStorageContractService.setValue(contractAddress, value, node, parseNodes(nodes)).map(r -> r.getBlockNumber()).blockingFirst();
            fail("Expected to fail");
        } catch (RuntimeException err) {
            TransactionException exception = (TransactionException) err.getCause().getCause();
            String revertReason = exception.getTransactionReceipt().get().getRevertReason().substring(REVERT_REASON_METHOD_ID.length());
            Utf8String decodedRevertReason = (Utf8String) FunctionReturnDecoder.decode(revertReason, REVERT_REASON_TYPES).get(0);
            assertThat(decodedRevertReason.getValue()).isEqualTo(reasonText);
        }
    }

    @Step("Get value from <node> matches <value>")
    public void validateValueOnContract(String node, int value) {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();

        // if a transactionHash has been stored then wait until node has executed it
        // TODO this is a temporary fix for particularly flaky tests where state is being read before it has been
        // updated - we should probably rework all tests to check that the state is ready before getting
        String transactionHash = DataStoreFactory.getScenarioDataStore().get("transactionHash").toString();
        transactionService.waitForTransactionReceipt(networkProperty.getQuorumNode(networkProperty.getNode(node)), transactionHash);

        BigInteger savedValue = increasingSimpleStorageContractService.getValue(contractAddress, node).blockingFirst();
        assertThat(savedValue.intValue()).isEqualTo(value);
    }
}
