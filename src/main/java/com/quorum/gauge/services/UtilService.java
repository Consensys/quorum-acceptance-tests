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

package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.NodeInfo;
import com.quorum.gauge.ext.PendingTransaction;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.NetPeerCount;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Service
public class UtilService extends AbstractService {

    public Observable<EthBlockNumber> getCurrentBlockNumber() {
        return getCurrentBlockNumberFrom(QuorumNode.Node1);
    }

    public Observable<EthBlockNumber> getCurrentBlockNumberFrom(Node node) {
        return getCurrentBlockNumberFrom(QuorumNode.valueOf(node.getName()));
    }

    public Observable<EthBlockNumber> getCurrentBlockNumberFrom(QuorumNode node) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return client.ethBlockNumber().flowable().toObservable();
    }

    public Observable<EthBlock> getBlockByNumber(QuorumNode node, int number) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return client.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(number)), false).flowable().toObservable();
    }

    public List<Transaction> getPendingTransactions(QuorumNode node) {
        Request<?, PendingTransaction> request = new Request<>(
                "eth_pendingTransactions",
                null,
                connectionFactory().getWeb3jService(node),
                PendingTransaction.class
        );

        return request.flowable().toObservable().blockingFirst().getTransactions();
    }

    public String getNodeInfoName(final QuorumNode node) {
        Request<?, NodeInfo> nodeInfoRequest = new Request<>(
            "admin_nodeInfo",
            null,
            connectionFactory().getWeb3jService(node),
            NodeInfo.class
        );

        NodeInfo nodeInfo = nodeInfoRequest.flowable().toObservable().blockingFirst();
        return nodeInfo.getName();
    }

    public Observable<Boolean> getRpcModules(QuorumNode node) {
        Request<?, GenericResponse> request = new Request<>(
            "rpc_modules",
            null,
            connectionFactory().getWeb3jService(node),
            GenericResponse.class
        );

        return request.flowable().toObservable().map(r -> !r.hasError());
    }

    /**
     * @param node
     * @return number of peers from the view of {@code node}
     */
    public int getNumberOfNodes(QuorumNode node) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        NetPeerCount peerCount = client.netPeerCount().flowable().toObservable().blockingFirst();

        return peerCount.getQuantity().intValue();
    }

    public static class GenericResponse extends Response<Map<String, Object>> {

    }
}
