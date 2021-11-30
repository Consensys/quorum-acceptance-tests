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
import com.quorum.gauge.ext.BatchRequest;
import com.quorum.gauge.ext.BatchResponse;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

import java.util.List;

/**
 * Generic service that can invoke JSON RPC APIs
 */
@Service
public class RPCService extends AbstractService {

    public <S, T extends Response> Observable<T> call(Node node, String method, List<S> params, Class<T> responseClass) {
        Request<S, T> request = new Request<>(
                method,
                params,
                connectionFactory().getWeb3jService(node),
                responseClass
        );
        return request.flowable().toObservable();
    }

    public Observable<BatchResponse> call(Node node, BatchRequest.Collector requestCollector) {
        Web3jService client = connectionFactory().getWeb3jService(node);
        return new BatchRequest(client, requestCollector.toList()).flowable().toObservable();
    }
}
