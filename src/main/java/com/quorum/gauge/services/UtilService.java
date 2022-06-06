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
import com.quorum.gauge.ext.PendingTransaction;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class UtilService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(UtilService.class);

    public Observable<EthBlockNumber> getCurrentBlockNumber() {
        return getCurrentBlockNumberFrom(QuorumNode.Node1);
    }

    public Observable<EthBlockNumber> getCurrentBlockNumberFrom(Node node) {
        return getCurrentBlockNumberFrom(QuorumNode.valueOf(node.getName()));
    }

    public void waitForNodesToReach(final BigInteger targetBlockHeight, final Node... nodes) {
        Observable.fromArray(nodes).flatMap(nodeName -> {
            var currentBlockHeight = getCurrentBlockNumberFrom(nodeName).blockingFirst().getBlockNumber();
            logger.debug(nodeName.getName() + " currentBlockHeight is " + currentBlockHeight + " target height is " + targetBlockHeight + " (" + (currentBlockHeight.compareTo(targetBlockHeight) >= 0) + ")");
            return Observable.just(currentBlockHeight.compareTo(targetBlockHeight) >= 0);
        }).doOnNext(ok -> {
            assertThat(ok).as("Node must start successfully").isTrue();
        }).observeOn(Schedulers.io()).retry(60, (x) -> {
            Thread.sleep(Duration.ofSeconds(1).toMillis());
            return true;
        }).blockingSubscribe();
        logger.debug("All nodes can be reached");
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
