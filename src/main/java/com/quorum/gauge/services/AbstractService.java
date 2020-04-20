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
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;

public abstract class AbstractService {

    BigInteger DEFAULT_GAS_LIMIT = new BigInteger("47b760", 16);
    BigInteger DEFAULT_PERMISSIONS_GAS_LIMIT = new BigInteger("8C6180", 16);
    int DEFAULT_SLEEP_DURATION_IN_MILLIS = 2000;
    int DEFAULT_MAX_RETRY = 30;

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
}
