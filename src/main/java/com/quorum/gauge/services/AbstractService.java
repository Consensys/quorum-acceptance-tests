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

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.EnhancedClientTransactionManager;
import com.quorum.gauge.ext.OpenQuorumTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.enclave.Enclave;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public abstract class AbstractService {

    public static final BigInteger DEFAULT_GAS_LIMIT = new BigInteger("47b760", 16);
    BigInteger DEFAULT_PERMISSIONS_GAS_LIMIT = new BigInteger("8C6180", 16);
    public static int DEFAULT_SLEEP_DURATION_IN_MILLIS = 2000;
    public static int DEFAULT_MAX_RETRY = 30;

    private ContractGasProvider permContractGasProvider = new PermissionContractGasProvider();
    private ContractGasProvider permContractDepGasProvider = new PermissionContractDeployGasProvider();

    @Autowired
    private QuorumNodeConnectionFactory connectionFactory;

    @Autowired
    private QuorumNetworkProperty networkProperty;

    protected QuorumNodeConnectionFactory connectionFactory() {
        if (Context.getConnectionFactory() == null) {
            return connectionFactory;
        }
        return Context.getConnectionFactory();
    }

    protected QuorumNetworkProperty networkProperty() {
        if (Context.getNetworkProperty() == null) {
            return networkProperty;
        }
        return Context.getNetworkProperty();
    }

    protected QuorumNetworkProperty.OAuth2ServerProperty oAuth2ServerProperty() {
        return  Optional.ofNullable(networkProperty().getOauth2Server())
                .orElseThrow(() -> new RuntimeException("missing oauth2 server configuration"));
    }

    public ContractGasProvider getPermContractGasProvider() {
        return permContractGasProvider;
    }

    public void setPermContractGasProvider(PermissionContractGasProvider permContractGasProvider) {
        this.permContractGasProvider = permContractGasProvider;
    }

    public ContractGasProvider getPermContractDepGasProvider() {
        return permContractDepGasProvider;
    }

    public void setPermContractDepGasProvider(ContractGasProvider permContractDepGasProvider) {
        this.permContractDepGasProvider = permContractDepGasProvider;
    }

    public ClientTransactionManager clientTransactionManager(Web3j web3j, String fromAddress, String privateFrom, List<String> privateFor) {
        return new EnhancedClientTransactionManager((Quorum) web3j, fromAddress, privateFrom, privateFor);
    }

    public org.web3j.tx.ClientTransactionManager vanillaClientTransactionManager(Web3j web3j, String fromAddress) {
        return new org.web3j.tx.ClientTransactionManager(web3j, fromAddress, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public RawTransactionManager rawTransactionManager(Web3j web3j, Credentials credentials, final long chainId) {
        return new RawTransactionManager(web3j, credentials, chainId, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public TransactionManager quorumTransactionManager(Quorum web3j,
                                                       Credentials credentials,
                                                       String privateFrom,
                                                       List<String> privateFor,
                                                       Enclave enclave) {


        return new OpenQuorumTransactionManager(web3j, credentials, privateFrom, privateFor, enclave, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }
}
