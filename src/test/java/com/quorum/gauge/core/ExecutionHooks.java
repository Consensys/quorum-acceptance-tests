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

package com.quorum.gauge.core;

import com.google.common.collect.ImmutableMap;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.InfrastructureService.NetworkResources;
import com.quorum.gauge.services.UtilService;
import com.thoughtworks.gauge.*;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ExecutionHooks {
    private static Logger logger = LoggerFactory.getLogger(ExecutionHooks.class);
    private static Logger stepLogger = LoggerFactory.getLogger(ExecutionHooks.class.getPackage().getName() + ".StepLogger");

    @Autowired
    QuorumNetworkProperty networkProperty;

    @Autowired
    UtilService utilService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    InfrastructureService infraService;

    private Map<String, AbstractConsumer> postConditionConsumers = ImmutableMap.of(
            "network-cleanup", new NetworkCleanupConsumer(99),
            "datadir-cleanup", new DatadirCleanupConsumer(1)
    );

    private Map<String, AbstractConsumer> preConditionConsumers = ImmutableMap.of(
    );

    @BeforeSpec
    public void beforeSpec(ExecutionContext context) {
    }

    @BeforeScenario
    public void beforeScenario(ExecutionContext context) {
        logger.debug("---> START OF BEFORE-SCENARIO");
        List<AbstractConsumer> consumers = context.getAllTags().stream()
                .filter(tag -> tag.startsWith("pre-condition"))
                .map(tag -> StringUtils.removeStart(tag, "pre-condition/"))
                .filter(preConditionConsumers::containsKey)
                .map(preConditionConsumers::get)
                .collect(Collectors.toList());
        if (!context.getAllTags().contains("pre-condition/no-record-blocknumber")) {
            consumers.add(new RecordBlockNumberConsumer(0));
        }
        // sort them asc by index
        consumers.sort(Comparator.comparingInt(o -> o.index));
        consumers.forEach(c -> c.accept(context));
        logger.debug("<--- END OF BEFORE-SCENARIO");
    }

    @AfterScenario
    public void afterScenario(ExecutionContext context) {
        logger.debug("---> START OF AFTER-SCENARIO");
        context.getAllTags().stream()
                .filter(tag -> tag.startsWith("post-condition"))
                .map(tag -> StringUtils.removeStart(tag, "post-condition/"))
                .filter(postConditionConsumers::containsKey)
                .map(postConditionConsumers::get)
                .sorted(Comparator.comparingInt(o -> o.index))
                .collect(Collectors.toList())
                .forEach(c -> c.accept(context));
        logger.debug("---> END OF AFTER-SCENARIO");
    }

    @BeforeSuite
    public void setNetworkProperties() {
        DataStoreFactory.getSuiteDataStore().put("networkProperties", networkProperty);
    }

    @BeforeStep
    public void beforeStepGlobal(ExecutionContext executionContext) {
        stepLogger.debug("--> STEP STARTS: {}", executionContext.getCurrentStep().getText());
    }

    @AfterStep
    public void afterStepGlobal() {
        stepLogger.debug("<-- STEP ENDS");
    }

    abstract class AbstractConsumer implements Consumer<ExecutionContext> {
        protected int index;
        protected AbstractConsumer(int index) {
            this.index = index;
        }
    }

    class NetworkCleanupConsumer extends AbstractConsumer {

        public NetworkCleanupConsumer(int index) {
            super(index);
        }

        @Override
        public void accept(ExecutionContext executionContext) {
            try {
                NetworkResources networkResources = (NetworkResources) DataStoreFactory.getScenarioDataStore().get("networkResources");
                infraService.deleteNetwork(networkResources).blockingSubscribe();
            } finally {
                DataStoreFactory.getScenarioDataStore().remove("networkResources");
            }
        }
    }

    /**
     * Delete existing datadirs
     */
    class DatadirCleanupConsumer extends AbstractConsumer {

        public DatadirCleanupConsumer(int index) {
            super(index);
        }

        @Override
        public void accept(ExecutionContext executionContext) {
            NetworkResources networkResources = (NetworkResources) DataStoreFactory.getScenarioDataStore().get("networkResources");
            infraService.deleteDatadirs(networkResources).blockingSubscribe();
        }
    }

    class RecordBlockNumberConsumer extends AbstractConsumer {

        public RecordBlockNumberConsumer(int index) {
            super(index);
        }

        @Override
        public void accept(ExecutionContext executionContext) {
            BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
            DataStoreFactory.getScenarioDataStore().put("blocknumber", currentBlockNumber);
        }
    }

    class NoOpsConsumer extends AbstractConsumer {
        public NoOpsConsumer() {
            super(0);
        }

        @Override
        public void accept(ExecutionContext executionContext) {
        }
    }
}
