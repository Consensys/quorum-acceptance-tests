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
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthChainId;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.Collections;

@Service
public class PublicRawSmartContract extends AbstractSpecImplementation {

    @Step("Deploy `SimpleStorage` public smart contract with initial value <initialValue> signed by external wallet <wallet> on node <node>, name this contract as <contractName>")
    public void deployClientReceiptSmartContract(Integer initialValue, WalletData wallet, QuorumNetworkProperty.Node node, String contractName) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        Contract c = rawContractService.createRawSimplePublicContract(initialValue, wallet, node, chainId).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName, c);
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> signed by external wallet <wallet> in <source>")
    public void updateNewValue(String contractName, int newValue, WalletData wallet, QuorumNetworkProperty.Node node) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = rawContractService.updateRawSimplePublicContract(node, wallet, c.getContractAddress(), newValue, chainId).blockingFirst();

        AssertionsForClassTypes.assertThat(receipt.getTransactionHash()).isNotBlank();
        AssertionsForClassTypes.assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }
}


