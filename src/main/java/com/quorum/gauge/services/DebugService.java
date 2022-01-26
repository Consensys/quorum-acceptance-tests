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

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.JsonResponse;
import com.quorum.gauge.ext.StringResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

import java.util.Arrays;
import java.util.Collections;

@Service
public class DebugService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(DebugService.class);

    public Observable<JsonResponse> traceTransaction(QuorumNetworkProperty.Node source, String txHash) {

        Request<?, JsonResponse> request = new Request<>(
            "debug_traceTransaction",
            Collections.singletonList(txHash),
            connectionFactory().getWeb3jService(source), JsonResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<StringResponse> privateStateRoot(QuorumNetworkProperty.Node source, String blockHeight) {

        Request<?, StringResponse> request = new Request<>(
            "debug_privateStateRoot",
            Collections.singletonList(blockHeight),
            connectionFactory().getWeb3jService(source), StringResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<StringResponse> defaultStateRoot(QuorumNetworkProperty.Node source, String blockHeight) {

        Request<?, StringResponse> request = new Request<>(
            "debug_defaultStateRoot",
            Collections.singletonList(blockHeight),
            connectionFactory().getWeb3jService(source), StringResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<JsonResponse> dumpAddress(QuorumNetworkProperty.Node source, String address, String blockNumber) {

        Request<?, JsonResponse> request = new Request<>(
            "debug_dumpAddress",
            Arrays.asList(address, blockNumber),
            connectionFactory().getWeb3jService(source), JsonResponse.class
        );

        return request.flowable().toObservable();
    }
}
