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

package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.QuorumNodeConnectionFactory;
import com.thoughtworks.gauge.Step;

import io.reactivex.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.quorum.Quorum;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class BlockReward extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(BlockReward.class);

    @Autowired
    private QuorumNodeConnectionFactory connectionFactory;


    @Step("From block <fromBlockNumber> to <toBlockNumber>, account <accountAddress> should see increase of <blockReward> per block")
    public void waitForBlockAndCheckRewardForAccount(int fromBlockNumber, int toBlockNumber, String accountAddress, int blockReward) throws IOException {
        assertThat(fromBlockNumber).isLessThan(toBlockNumber);
        var currentBlockHeight = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue();
        waitForBlockHeight(currentBlockHeight, toBlockNumber);

        QuorumNode node = QuorumNode.Node1;

        int blockNumber = toBlockNumber;
        while (blockNumber > fromBlockNumber) {
            // get balance at some block
            Quorum connection = connectionFactory.getConnection(node);
            EthGetBalance previousBalance = connection
                .ethGetBalance(accountAddress, DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber-1)))
                .send();

            EthGetBalance currentBalance = connection
                .ethGetBalance(accountAddress, DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)))
                .send();

            Error currentBalanceError = currentBalance.getError();
            if (currentBalanceError != null) {
                logger.error("balance-1: "+currentBalanceError.getCode() + ": " +currentBalanceError.getData() + ": " + currentBalanceError.getMessage());
            }
            Error previousBalanceError = previousBalance.getError();
            if (previousBalanceError != null) {
                logger.error("balance: "+previousBalanceError.getCode() + ": " +previousBalanceError.getData() + ": " + previousBalanceError.getMessage());
            }

            BigInteger delta = currentBalance.getBalance().subtract(previousBalance.getBalance());
            if (delta.longValue() != blockReward) {
                logger.info("reward of "+delta+" is not correct for block #"+blockNumber+" should be "+blockReward);
            }
            assertThat(delta).isEqualTo(BigInteger.valueOf(blockReward));
            blockNumber--;
        }
    }
}
