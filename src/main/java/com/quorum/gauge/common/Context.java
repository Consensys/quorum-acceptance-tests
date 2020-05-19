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

import com.quorum.gauge.services.QuorumNodeConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    static ThreadLocal<QuorumNodeConnectionFactory> connectionFactoryThreadLocal = new ThreadLocal<>();
    static ThreadLocal<String> accessTokenHolder = new ThreadLocal<>();

    public static void setConnectionFactory(QuorumNodeConnectionFactory c) {
        if (c == null) {
            return;
        }
        logger.debug("Setting connection factory for thread {}, with nodes {}", Thread.currentThread().getName(), c.getNetworkProperty().getNodes());
        connectionFactoryThreadLocal.set(c);
    }

    public static QuorumNodeConnectionFactory getConnectionFactory() {
        return connectionFactoryThreadLocal.get();
    }

    public static QuorumNetworkProperty getNetworkProperty() {
        return getConnectionFactory() == null ? null : getConnectionFactory().getNetworkProperty();
    }

    public static void clear() {
        connectionFactoryThreadLocal.remove();
    }

    public static String storeAccessToken(String token) {
        accessTokenHolder.set(token);
        return token;
    }

    public static String retrieveAccessToken() {
        return accessTokenHolder.get();
    }

    public static void removeAccessToken() {
        accessTokenHolder.remove();
    }
}
