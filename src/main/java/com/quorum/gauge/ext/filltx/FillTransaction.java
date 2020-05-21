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

import org.web3j.protocol.core.methods.response.Transaction;

import java.util.List;

public class FillTransaction {
    String raw;
    Transaction tx;

    public FillTransaction() {
    }

    public FillTransaction(String raw, Transaction tx) {
        this.raw = raw;
        this.tx = tx;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    @Override
    public String toString() {
        return "FillTransaction{" +
            "raw='" + raw + '\'' +
            ", tx=" + tx +
            '}';
    }

    public PrivateFillTransaction getPrivateTransaction(String from, List<String> privateFor) {
        PrivateFillTransaction txObject =
            new PrivateFillTransaction(
                from,
                tx.getNonce(),
                tx.getGas(),
                null,
                tx.getValue(),
                tx.getInput(),
                null,
                privateFor);

        return txObject;
    }
}
