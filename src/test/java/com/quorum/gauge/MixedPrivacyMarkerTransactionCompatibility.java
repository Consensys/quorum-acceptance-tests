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

import com.quorum.gauge.common.GethArgBuilder;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.InfrastructureService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class MixedPrivacyMarkerTransactionCompatibility extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(BlockSynchronization.class);

    @Autowired
    private InfrastructureService infraService;

    @Step("Start the PMT network with: <table>")
    public void startNetwork(Table table) {
        InfrastructureService.NetworkResources networkResources = new InfrastructureService.NetworkResources();
        try {
            Observable.fromIterable(table.getTableRows())
                .flatMap(r -> infraService.startNode(
                    InfrastructureService.NodeAttributes.forNode(r.getCell("node"))
                        .withAdditionalGethArgs(GethArgBuilder.newBuilder().privacyMarkerEnable(Boolean.parseBoolean(r.getCell("--privacymarker.enable")))),
                    resourceId -> networkResources.add(r.getCell("node"), resourceId)))
                .doOnNext(ok -> {
                    assertThat(ok).as("Node must start successfully").isTrue();
                })
                .doOnComplete(() -> {
                    logger.debug("Waiting for network to be up completely...");
                    Thread.sleep(networkProperty.getConsensusGracePeriod().toMillis());
                })
                .blockingSubscribe();
        } finally {
            DataStoreFactory.getScenarioDataStore().put("networkResources", networkResources);
        }
    }



}
