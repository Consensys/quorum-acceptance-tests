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
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStore;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.assertj.core.data.Percentage;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.FunctionEncoder;
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class EstimateGas extends AbstractSpecImplementation {

    @Step("Estimate gas for public transaction transferring some Wei from a default account in <from> to a default account in <to>")
    public void estimatePublicTransaction(QuorumNode from, QuorumNode to) {
        EthEstimateGas estimatedValue = transactionService.estimateGasForTransaction(new Random().nextInt(10) + 1, from, to).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }

    @Step("Estimate gas for public transaction transferring zero Wei from a default account in <from> to a default account in <to>")
    public void estimatePublicTransactionZeroValue(QuorumNode from, QuorumNode to) {
        EthEstimateGas estimatedValue = transactionService.estimateGasForTransaction(0, from, to).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }

    @Step("Deploy `SimpleContract` public smart contract from a default account in <from>")
    public void createContract(QuorumNode from) {
        Contract c = contractService.createSimpleContract(0, from, null).blockingFirst();

        DataStoreFactory.getSpecDataStore().put("publicContract1", c);
    }

    private static String getEncodedContractDeployData(Contract c, int initVal) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(initVal)));
        return c.getContractBinary() + encodedConstructor;
    }

    @Step("Estimate gas for deploying `SimpleContract` public smart contract from a default account in <from>")
    public void estimatePublicContract(QuorumNetworkProperty.Node from) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), "publicContract1", Contract.class);

        String encodedData = getEncodedContractDeployData(c, 0);
        EthEstimateGas estimatedValue = transactionService.estimateGasForPublicContract(from, encodedData).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }

    @Step("Estimate gas for calling the `SimpleContract` public smart contract from a default account in <from>")
    public void estimatePublicContractCall(QuorumNode from) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), "publicContract1", Contract.class);

        EthEstimateGas estimatedValue = transactionService.estimateGasForPublicContractCall(from, c).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }


    @Step("Deploy `SimpleContract` private smart contract from a default account in <from> and private for <privateFor>")
    public void createPrivateContract(QuorumNode from, QuorumNode privateFor) {
        Contract c = contractService.createSimpleContract(0, from, privateFor).blockingFirst();

        DataStoreFactory.getSpecDataStore().put("privateContract1", c);
    }

    @Step("Estimate gas for deploying `SimpleContract` private smart contract from a default account in <from> and private for <privateFor>")
    public void estimatePrivateContract(QuorumNode from, QuorumNode privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), "privateContract1", Contract.class);

        String encodedData = getEncodedContractDeployData(c, 0);
        EthEstimateGas estimatedValue = transactionService.estimateGasForPrivateContract(from, privateFor, encodedData).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }

    @Step("Estimate gas for calling the `SimpleContract` private smart contract from a default account in <from> and private for <privateFor>")
    public void estimatePrivateContractCall(QuorumNode from, QuorumNode privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), "privateContract1", Contract.class);

        EthEstimateGas estimatedValue = transactionService.estimateGasForPrivateContractCall(from, privateFor, c).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("estimatedValue", estimatedValue);
    }

    @Step("Gas estimate <expectedValue> is returned within <tolerance> percent")
    public void verifyEstimate(String expectedValue, String tolerance) {
        EthEstimateGas estimatedValue = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "estimatedValue", EthEstimateGas.class);

        Double percentage = Double.valueOf(tolerance);
        try {
            assertThat(estimatedValue.getAmountUsed()).isCloseTo(new BigInteger(expectedValue), Percentage.withPercentage(percentage));
        } catch (MessageDecodingException e) {
            fail("Invalid estimate was returned which cannot be interpreted: '%s'", estimatedValue.getRawResponse());
        }

    }

    @Step("Update contract <contractName> with value <value>, from <from> to <privateFor> using estimated gas")
    public void updateContractUsingEstimatedGas(final String contractName, final int value,
                                                final QuorumNode from, final QuorumNode privateFor) {
        final DataStore specStore = DataStoreFactory.getSpecDataStore();
        final DataStore scenarioStore = DataStoreFactory.getScenarioDataStore();

        final Contract contract = mustHaveValue(specStore, contractName, Contract.class);
        final String contractAddress = contract.getContractAddress();

        final EthEstimateGas estimatedGasResult = mustHaveValue(scenarioStore, "estimatedValue", EthEstimateGas.class);
        final BigInteger estimatedGasLimit = estimatedGasResult.getAmountUsed();

        final TransactionReceipt receipt = contractService
            .updateSimpleContractWithGasLimit(from, Arrays.asList(privateFor), contractAddress, estimatedGasLimit, value,
                PrivacyFlag.STANDARD_PRIVATE)
            .blockingFirst();

        assertThat(receipt.isStatusOK()).isTrue();
    }

}
