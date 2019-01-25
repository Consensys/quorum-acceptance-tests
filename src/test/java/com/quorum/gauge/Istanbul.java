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
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class Istanbul extends AbstractSpecImplementation {
    @Autowired
    IstanbulService istanbulService;

    @Step({"The consensus should work at the beginning", "The consensus should work after resuming"})
    public void verifyConsensus() {
        int diff = 3;
        // wait for blockheight increases by 3 from the current one
        waitForBlockHeight(currentBlockNumber().intValue(), currentBlockNumber().intValue() + diff);
    }

    @Step("Among <total> validators, stop <x> validators so there are less than 2F + 1 validators in the network")
    public void stopValidators(int total, int x) {
        List<QuorumNode> nodes = Observable.from(QuorumNode.values()).take(total).toList().toBlocking().first();
        Collections.shuffle(nodes);
        List<QuorumNode> stoppedNodes = nodes.subList(0, x);
        BigInteger lastBlockNumber = Observable.from(stoppedNodes)
                .flatMap(node -> istanbulService.stopMining(node).subscribeOn(Schedulers.io()))
                .flatMap(s -> utilService.getCurrentBlockNumber())
                .toBlocking().last().getBlockNumber();

        DataStoreFactory.getScenarioDataStore().put("stoppedNodes", stoppedNodes);
        DataStoreFactory.getScenarioDataStore().put("lastBlockNumber", lastBlockNumber);
    }

    @ContinueOnFailure
    @Step("The consensus should stop")
    public void verifyConsensusStopped() {
        BigInteger lastBlockNumber = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "lastBlockNumber", BigInteger.class);
        // wait for 10 seconds and get the block number
        BigInteger newBlockNumber = Observable.timer(10, TimeUnit.SECONDS).flatMap(x -> utilService.getCurrentBlockNumber()).toBlocking().first().getBlockNumber();

        assertThat(newBlockNumber).isEqualTo(lastBlockNumber);
    }

    @Step("Resume the stopped validators")
    public void startValidators() {
        List<QuorumNode> nodes = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "stoppedNodes", List.class);
        Observable.from(nodes)
                .flatMap(node -> istanbulService.startMining(node).subscribeOn(Schedulers.io()))
                .toBlocking().first();
    }
}
