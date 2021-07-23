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
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.IstanbulService;
import com.thoughtworks.gauge.ContinueOnFailure;
import com.thoughtworks.gauge.Gauge;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class Istanbul extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(Istanbul.class);

    @Autowired
    IstanbulService istanbulService;

    @Step({"The consensus should work at the beginning", "The consensus should work after resuming", "The consensus should work after stopping 2F validators"})
    public void verifyConsensus() {
        int diff = 3;
        // wait for blockheight increases by 3 from the current one
        waitForBlockHeight(currentBlockNumber().intValue(), currentBlockNumber().intValue() + diff);
    }

    @Step("Among all validators, stop some validators so there are less than 2F + 1 validators in the network")
    public void stopValidators() {
        int totalNodesLive = utilService.getNumberOfNodes(QuorumNode.Node1) + 1;
        int totalNodesConfigured = numberOfQuorumNodes();
        int numOfValidatorsToStop = Math.round((totalNodesLive - 1) / 2.0f);
        // we only can stop validators that are configured
        assertThat(numOfValidatorsToStop).describedAs("Not enough configured validators to perform STOP operation").isLessThanOrEqualTo(totalNodesConfigured);

        Gauge.writeMessage(String.format("Stopping %d validators from total of %d validators", numOfValidatorsToStop, totalNodesLive));

        final QuorumNode[] quorumNodes = networkProperty.getNodes().keySet().stream().map(QuorumNode::valueOf).toArray(QuorumNode[]::new);
        List<QuorumNode> nodes = Observable.fromArray(quorumNodes).take(totalNodesConfigured).toList().blockingGet();
        Collections.shuffle(nodes);
        List<QuorumNode> stoppedNodes = nodes.subList(0, numOfValidatorsToStop);
        Observable.fromIterable(stoppedNodes)
                .flatMap(istanbulService::stopMining)
                .blockingSubscribe();

        DataStoreFactory.getScenarioDataStore().put("stoppedNodes", stoppedNodes);
    }

    @Step("Among all validators, stop some validators so there are 2F validators in the network")
    public void stop2FValidators() {
        int totalNodesLive = utilService.getNumberOfNodes(QuorumNode.Node1) + 1;
        int totalNodesConfigured = numberOfQuorumNodes();
        int numOfValidatorsToStop = Math.round((totalNodesLive) / 2.0f);
        // we only can stop validators that are configured
        assertThat(numOfValidatorsToStop).describedAs("Not enough configured validators to perform STOP operation").isLessThanOrEqualTo(totalNodesConfigured);

        Gauge.writeMessage(String.format("Stopping %d validators from total of %d validators", numOfValidatorsToStop, totalNodesLive));

        List<QuorumNode> nodes = Observable.fromArray(QuorumNode.values()).take(totalNodesConfigured).toList().blockingGet();
        Collections.shuffle(nodes);
        List<QuorumNode> stoppedNodes = nodes.subList(0, numOfValidatorsToStop);
        Observable.fromIterable(stoppedNodes)
            .flatMap(istanbulService::stopMining)
            .blockingSubscribe();

        DataStoreFactory.getScenarioDataStore().put("stoppedNodes", stoppedNodes);
    }

    @ContinueOnFailure
    @Step("The consensus should stop")
    public void verifyConsensusStopped() {
        BigInteger lastBlockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
        // wait for 10 seconds and get the block number
        BigInteger newBlockNumber = Observable.timer(10, TimeUnit.SECONDS).flatMap(x -> utilService.getCurrentBlockNumber()).blockingFirst().getBlockNumber();

        assertThat(newBlockNumber).isEqualTo(lastBlockNumber);
    }

    @Step("Resume the stopped validators")
    public void startValidators() {
        List<QuorumNode> nodes = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "stoppedNodes", List.class);
        Observable.fromIterable(nodes)
                .flatMap(istanbulService::startMining)
                .blockingSubscribe();
    }
}
