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
import com.quorum.gauge.common.QuorumNode;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AccountService extends AbstractService {

    public Observable<String> getAccountAddresses(QuorumNode node) {
        return connectionFactory()
                .getConnection(node)
                .ethAccounts()
                .flowable()
                .toObservable()
                .flatMap(ethAccounts -> Observable.fromIterable(ethAccounts.getAccounts()));
    }

    public Observable<String> getDefaultAccountAddress(QuorumNode node) {
        QuorumNetworkProperty.Node qnode = networkProperty().getNode(node.name());
        if (qnode != null) {
            return getDefaultAccountAddress(qnode);
        } else {
            return getAccountAddresses(node).firstOrError().toObservable();
        }
    }

    public Observable<String> getDefaultAccountAddress(QuorumNetworkProperty.Node node) {
        Map<String, String> accountAliases = node.getAccountAliases();
        if (CollectionUtils.isEmpty(accountAliases) || !accountAliases.containsKey("Default")) {
            return Observable.just(accountAliases.values().iterator().next());
        } else {
            return Observable.just(accountAliases.get("Default"));
        }
    }

    public Observable<EthGetBalance> getDefaultAccountBalance(QuorumNode node) {
        return getDefaultAccountAddress(node)
            .flatMap(s -> connectionFactory()
                .getConnection(node)
                .ethGetBalance(s, DefaultBlockParameterName.LATEST)
                .flowable()
                .toObservable()
            );
    }

    public Observable<ListWalletsResponse> personalListWallets(QuorumNetworkProperty.Node node) {
        Request<?, ListWalletsResponse> request = new Request<>(
            "personal_listWallets",
            Collections.<String>emptyList(),
            connectionFactory().getWeb3jService(node),
            ListWalletsResponse.class);

        return request.flowable().toObservable();
    }

    /**
     *
     * @param alias
     * @return account address in the network property
     */
    public String address(String alias) {
        for (QuorumNetworkProperty.Node node : networkProperty().getNodes().values()) {
            if (node.getAccountAliases().containsKey(alias)) {
                return node.getAccountAliases().get(alias);
            }
        }
        throw new IllegalArgumentException("no such alias in the network property: " + alias);
    }

    public Observable<String> getAccountAddress(QuorumNetworkProperty.Node source, String ethAccount) {
        if (ethAccount == null) {
            return getDefaultAccountAddress(source);
        }
        // we don't look up for the address in source Node as we want
        // to serve -ve cases
        for (QuorumNetworkProperty.Node node : networkProperty().getNodes().values()) {
            if (node.getAccountAliases().containsKey(ethAccount)) {
                return Observable.just(node.getAccountAliases().get(ethAccount));
            }
        }
        throw new IllegalArgumentException("no such account alias: " + ethAccount);
    }

    public static class ListWalletsResponse extends Response<List<Wallet>> {
        public List<Wallet> getWallets() {
            return getResult();
        }
    }

    public static class Wallet {
        List<Accounts> accounts;
        String url;
        String status;
        String failure;

        public boolean contains(String address) {
            if (accounts != null) {
                for (AccountService.Accounts a : getAccounts()) {
                    if (address.equals(a.getAddress())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public List<Accounts> getAccounts() {
            return accounts;
        }

        public void setAccounts(List<Accounts> accounts) {
            this.accounts = accounts;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getFailure() {
            return failure;
        }

        public void setFailure(String failure) {
            this.failure = failure;
        }
    }

    public static class Accounts {
        String address;
        String url;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

}
