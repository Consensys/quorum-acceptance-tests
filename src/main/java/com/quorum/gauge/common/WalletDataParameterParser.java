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

package com.quorum.gauge.common;

import com.quorum.gauge.common.config.WalletData;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import com.thoughtworks.gauge.execution.parameters.parsers.base.CustomParameterParser;
import gauge.messages.Spec;

public class WalletDataParameterParser extends CustomParameterParser<WalletData> {

    @Override
    protected WalletData customParse(final Class<?> aClass, final Spec.Parameter parameter) {
        final String walletName = parameter.getValue();

        final QuorumNetworkProperty props
            = (QuorumNetworkProperty) DataStoreFactory.getSuiteDataStore().get("networkProperties");

        final WalletData convertedWallet = props.getWallets().get(walletName);
        if(convertedWallet == null) {
            throw new IllegalArgumentException("Wallet " + walletName + " not found in network properties");
        }
        return convertedWallet;
    }

    @Override
    public boolean canParse(final Class<?> aClass, final Spec.Parameter parameter) {
        return aClass.equals(WalletData.class);
    }
}
