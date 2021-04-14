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

import com.quorum.gauge.common.PrivacyFlag;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.tx.ClientTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static com.quorum.gauge.services.AbstractService.DEFAULT_MAX_RETRY;
import static com.quorum.gauge.services.AbstractService.DEFAULT_SLEEP_DURATION_IN_MILLIS;

/**
 * Support additional privacy constructs
 *
 * @see org.web3j.quorum.tx.ClientTransactionManager
 * @see PrivateContractFlag
 */
public class EnhancedClientTransactionManager extends ClientTransactionManager {

    private List<PrivacyFlag> contractFlag;

    private Quorum quorum;

    public EnhancedClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, List<PrivacyFlag> contractFlag, int attempts, int sleepDuration) {
        super(quorum, fromAddress, privateFrom, privateFor, attempts, sleepDuration);
        this.quorum = quorum;
        this.contractFlag = contractFlag;
    }

    public EnhancedClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, List<PrivacyFlag> contractFlag) {
        this(quorum, fromAddress, privateFrom, privateFor, contractFlag, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        PrivateTransaction tx;
        if (contractFlag != null) {
            tx = new EnhancedPrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor(), contractFlag);
        } else {
            tx = new PrivateTransaction(getFromAddress(), null, gasLimit, to, value, data, getPrivateFrom(), getPrivateFor());
        }
        return quorum.ethSendTransaction(tx).send();
    }

    public static class EnhancedPrivateTransaction extends PrivateTransaction {

        private int privacyFlag;

        public EnhancedPrivateTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, List<PrivacyFlag> flags) {
            super(from, nonce, gasLimit, to, value, data, privateFrom, privateFor);
            int flag = 0;
            for (PrivacyFlag f : flags) {
                flag = flag | f.intValue();
            }
            this.privacyFlag = flag;
        }

        public int getPrivacyFlag() {
            return privacyFlag;
        }

        public void setPrivacyFlag(int privacyFlag) {
            this.privacyFlag = privacyFlag;
        }
    }
}
