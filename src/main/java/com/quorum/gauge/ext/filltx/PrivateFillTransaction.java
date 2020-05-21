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

package com.quorum.gauge.ext.filltx;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.List;

public class PrivateFillTransaction extends Transaction {
    private String privateFrom;
    private List<String> privateFor;

    public PrivateFillTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor) {
        super(from, nonce, BigInteger.ZERO, gasLimit, to, value, data);
        this.privateFrom = privateFrom;
        this.privateFor = privateFor;
    }

    public String getPrivateFrom() {
        return this.privateFrom;
    }

    public void setPrivateFrom(String privateFrom) {
        this.privateFrom = privateFrom;
    }

    public List<String> getPrivateFor() {
        return this.privateFor;
    }

    public void setPrivateFor(List<String> privateFor) {
        this.privateFor = privateFor;
    }
}
