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

import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.tx.ClientTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * Support additional privacy constructs
 *
 * @see org.web3j.quorum.tx.ClientTransactionManager
 * @see PrivateContractFlag
 */
public class EnhancedClientTransactionManager extends ClientTransactionManager {

    private PrivateContractFlag contractFlag;
    private Quorum quorum;

    public EnhancedClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, PrivateContractFlag contractFlag, int attempts, int sleepDuration) {
        super(quorum, fromAddress, privateFrom, privateFor, attempts, sleepDuration);
        this.quorum = quorum;
        this.contractFlag = contractFlag;
    }

    public PrivateContractFlag getContractFlag() {
        return contractFlag;
    }

    public void setContractFlag(PrivateContractFlag contractFlag) {
        this.contractFlag = contractFlag;
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        EnhancedPrivateTransaction tx = new EnhancedPrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor(), contractFlag);
        return quorum.ethSendTransaction(tx).send();
    }

    public static class EnhancedPrivateTransaction extends PrivateTransaction {

        private boolean psv;

        public EnhancedPrivateTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, PrivateContractFlag flag) {
            super(from, nonce, gasLimit, to, value, data, privateFrom, privateFor);
            this.psv = flag == PrivateContractFlag.PSV;
        }

        public boolean isPsv() {
            return psv;
        }

        public void setPsv(boolean psv) {
            this.psv = psv;
        }
    }
}
