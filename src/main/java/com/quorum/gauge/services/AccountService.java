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
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
        return Observable.just(getAccountAddresses(node).blockingFirst());
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

    public List<Wallet> personalListWallets(QuorumNetworkProperty.Node node) throws IOException {
        Request<?, ListWalletsResponse> request = new Request<>(
            "personal_listWallets",
            Collections.<String>emptyList(),
            connectionFactory().getWeb3jService(node),
            ListWalletsResponse.class);

        return request.send().getWallets();
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
                    };
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
