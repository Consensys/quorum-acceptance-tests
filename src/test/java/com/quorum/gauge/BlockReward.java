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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class BlockReward extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(BlockReward.class);

    @Autowired
    private QuorumNodeConnectionFactory connectionFactory;

    class Tuple {
        int blockReward;
        EthBlock.Block from;
        EthBlock.Block to;
        int delta;
        Tuple(int blockReward, EthBlock.Block from, EthBlock.Block to, int delta) {
            this.blockReward = blockReward;
            this.from = from;
            this.to = to;
            this.delta = delta;
        }
        public String toString() {
            return "["+from.getNumber().intValue()+"("+from.getTransactions().isEmpty()+") to "+to.getNumber().intValue()+"("+to.getTransactions().isEmpty()+")] "+blockReward+" "+delta;
        }
    }

    @Step("From block <fromblocknumber> to <toblocknumber>, account <accountAddress> should see increase of <blockReward> per block")
    public void waitForBlockAndCheckRewardForAccount(int fromblocknumber, int toblocknumber, String accountAddress, int blockReward) {
        assertThat(fromblocknumber).isLessThan(toblocknumber);
        while (utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue() < toblocknumber) {
            logger.info("sleep a bit, current block #"+utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue()+" expected is #"+toblocknumber);
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {}
        }
        QuorumNode node = QuorumNode.Node1;

        int number = toblocknumber;
        while (number > fromblocknumber) {
            // get balance at some block
            accountService.getDefaultAccountBalance(node);
            Quorum connection = connectionFactory.getConnection(node);
            Observable<EthGetBalance> bNm1 = connection
                .ethGetBalance(accountAddress, DefaultBlockParameter.valueOf(BigInteger.valueOf(number-1)))
                .flowable()
                .toObservable();
            Observable<EthGetBalance> bN = connection
                .ethGetBalance(accountAddress, DefaultBlockParameter.valueOf(BigInteger.valueOf(number)))
                .flowable()
                .toObservable();
            EthGetBalance bNm1V = bNm1.blockingFirst();
            EthGetBalance bNV = bN.blockingFirst();
            Error errNm1V = bNm1V.getError();
            if (errNm1V != null) {
                logger.error("balance-1: "+errNm1V.getCode() + ": " +errNm1V.getData() + ": " + errNm1V.getMessage());
            }
            Error errNV = bNV.getError();
            if (errNV != null) {
                logger.error("balance: "+errNV.getCode() + ": " +errNV.getData() + ": " + errNV.getMessage());
            }
            BigInteger delta = bNV.getBalance().subtract(bNm1V.getBalance());
            if (delta.longValue() != blockReward) {
                logger.info("reward of "+delta+" is not correct for block #"+number+" should be "+blockReward);
            }
            assertThat(delta).isEqualTo(BigInteger.valueOf(blockReward));
            number--;
        }
    }

    @Step("From block <fromblocknumber> to <toblocknumber>, <nodeId> account should see increase of <blockReward> per block")
    public void waitForBlockAndCheckRewardForNode(int fromblocknumber, int toblocknumber, String nodeId, int blockReward) {
        String accountAddress = accountService.getDefaultAccountAddress(QuorumNode.valueOf(nodeId)).blockingFirst();
        waitForBlockAndCheckRewardForAccount(fromblocknumber, toblocknumber, accountAddress, blockReward);
    }
}
