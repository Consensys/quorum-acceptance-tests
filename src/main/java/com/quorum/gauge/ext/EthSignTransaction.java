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

package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

import java.util.Map;
import java.util.Optional;

public class EthSignTransaction extends Response<Object> {

    public Optional<String> getRaw() {
        /**
         * Geth (at least up to v1.10.1) returns an object with raw and tx (check https://github.com/ethereum/go-ethereum/blob/9c653ff6625df1ff0f6c6612958b2a1da0021e40/internal/ethapi/api.go#L1821)
         * However, the official spec https://eth.wiki/json-rpc/API details that eth_signTransaction returns only the hash.
         *
         * This code is to support both behaviours while Geth is not fixed
         */

        Optional<Object> result = Optional.ofNullable(getResult());

        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (result.get() instanceof String) {
            return Optional.of((String) result.get());
        }

        Map<String, Object> map = (Map<String, Object>) result.get();
        return Optional.of((String) map.get("raw"));
    }
}
