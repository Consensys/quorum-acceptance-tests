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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.ArrayList;

public class PendingTransaction extends Response<ArrayList> {
    private static final Logger logger = LoggerFactory.getLogger(PendingTransaction.class);

    @SuppressWarnings("unchecked")
    public ArrayList<Transaction> getTransactions() {
        if (getResult() == null) {
            return null;
        }

        if (getResult() instanceof ArrayList) {
            ArrayList<Transaction> result = getResult();
            return result;
        }

        throw new RuntimeException("Unexpected error: expected PendingTransaction response to be of type ArrayList, but got: " + getResult().getClass());
    }

}