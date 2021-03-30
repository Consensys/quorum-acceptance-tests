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
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.assertj.core.api.AssertionsForClassTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@Service
public class PrivateRawSmartContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(PrivateRawSmartContract.class);

    @Step("Deploy a simple smart contract with initial value <initialValue> signed by external wallet <wallet> in <source> and it's private for <target>, name this contract as <contractName>")
    public void setupContract(int initialValue, WalletData wallet, QuorumNode source, QuorumNode target, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = rawContractService.createRawSimplePrivateContract(initialValue, wallet, source, target).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> signed by external wallet <wallet> in <source> and it's private for <target>")
    public void updateNewValue(String contractName, int newValue, WalletData wallet, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = rawContractService.updateRawSimplePrivateContract(newValue, c.getContractAddress(), wallet, source, target).blockingFirst();

        AssertionsForClassTypes.assertThat(receipt.getTransactionHash()).isNotBlank();
        AssertionsForClassTypes.assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Transaction Receipt is present in <node> for <contractName> from external wallet <wallet>")
    public void verifyTransactionReceipt(QuorumNetworkProperty.Node node, String contractName, WalletData wallet) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);
        Optional<TransactionReceipt> receipt = transactionService.pollTransactionReceipt(node, transactionHash);

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());

        final Credentials[] credentials = new Credentials[1];
        assertThatCode(() -> credentials[0] = WalletUtils.loadCredentials(wallet.getWalletPass(), wallet.getWalletPath()))
            .doesNotThrowAnyException();

        assertThat(receipt.get().getFrom()).isEqualTo(credentials[0].getAddress());
    }

}
