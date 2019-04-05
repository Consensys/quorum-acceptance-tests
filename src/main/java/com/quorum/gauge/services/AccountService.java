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
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import rx.Observable;

@Service
public class AccountService extends AbstractService {

    public Observable<String> getAccountAddresses(QuorumNode node) {
        return connectionFactory()
                .getConnection(node)
                .ethAccounts()
                .observable()
                .flatMap(ethAccounts -> Observable.from(ethAccounts.getAccounts()));
    }

    public Observable<String> getDefaultAccountAddress(QuorumNode node) {
        return getAccountAddresses(node).first();
    }

    public Observable<EthGetBalance> getDefaultAccountBalance(QuorumNode node) {
        return getDefaultAccountAddress(node)
            .flatMap(s -> connectionFactory()
                .getConnection(node)
                .ethGetBalance(s, DefaultBlockParameterName.LATEST)
                .observable()
            );
    }

}
