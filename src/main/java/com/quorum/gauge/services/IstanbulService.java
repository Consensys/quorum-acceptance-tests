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

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.IstanbulPropose;
import com.quorum.gauge.ext.MinerStartStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;

@Service
public class IstanbulService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(IstanbulService.class);

    public Observable<MinerStartStop> stopMining(QuorumNode node) {
        logger.debug("Request {} to stop mining", node);

        Request<?, MinerStartStop> request = new Request<>(
                "miner_stop",
                null,
                connectionFactory().getWeb3jService(node),
                MinerStartStop.class
        );

        return request.observable();
    }

    public Observable<MinerStartStop> startMining(QuorumNode node) {
        logger.debug("Request {} to start mining", node);

        Request<?, MinerStartStop> request = new Request<>(
                "miner_start",
                Collections.emptyList(),
                connectionFactory().getWeb3jService(node),
                MinerStartStop.class
        );

        return request.observable();
    }

    public Observable<IstanbulPropose> propose(QuorumNode node, String proposedValidatorAddress) {
        logger.debug("Node {} proposing {}", node, proposedValidatorAddress);

        Request<?, IstanbulPropose> request = new Request<>(
                "istanbul_propose",
                Arrays.asList(proposedValidatorAddress, true),
                connectionFactory().getWeb3jService(node),
                IstanbulPropose.class
        );

        return request.observable();
    }

}
