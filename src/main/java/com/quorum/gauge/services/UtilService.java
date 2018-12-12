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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Transaction;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

@Service
public class UtilService extends AbstractService {

    public Observable<EthBlockNumber> getCurrentBlockNumber() {
        return getCurrentBlockNumberFrom(QuorumNode.Node1);
    }

    public Observable<EthBlockNumber> getCurrentBlockNumberFrom(QuorumNode node) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        return client.ethBlockNumber().observable();
    }

    //TODO: this doesn't seem to work (it doesn't return any pending transactions)
    public List<Transaction> getPendingHashes(QuorumNode node) {
        Web3j client = connectionFactory().getWeb3jConnection(node);
        List<Transaction> transactions = new ArrayList<>();
        client.pendingTransactionObservable().subscribe(tx -> {
            transactions.add(tx);
        }, Throwable::printStackTrace);
        return transactions;
    }
}
