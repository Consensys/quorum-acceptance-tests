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
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Chain extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(Chain.class);

    @Step("Capture the current block height, named it as <snapshotName>")
    public void captureCurrentBlockHeight(String snapshotName) {
        DataStoreFactory.getScenarioDataStore().put(snapshotName, currentBlockNumber());
    }

    @Step("Wait until block height is atleast <n> higher than <snapshotName>")
    public void waitUntilBlockHeight(int n, String snapshotName) {
        AtomicInteger lastBlockHeight = new AtomicInteger(((BigInteger) DataStoreFactory.getScenarioDataStore().get(snapshotName)).intValue());
        int untilBlockHeight = n + lastBlockHeight.get();
        utilService.getCurrentBlockNumber().flatMap(ethBlockNumber -> {
            if (ethBlockNumber.getBlockNumber().intValue() < untilBlockHeight) {
                logger.debug("Current block height is {}, wait until {}", ethBlockNumber.getBlockNumber(), untilBlockHeight);
                lastBlockHeight.set(ethBlockNumber.getBlockNumber().intValue());
                throw new RuntimeException("let's wait and retry");
            }
            return Observable.just(true);
        }).retryWhen(
            attempts -> attempts.zipWith(Observable.range(1, n), (total, i) -> i)
            .flatMap(i -> Observable.timer(3, TimeUnit.SECONDS))
            .doOnCompleted(() -> {
                throw new RuntimeException("Timed out! Can't wait until block height is " + n + " higher. Last block height was " + lastBlockHeight.get());
            })
        ).toBlocking().first();
    }
}
