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

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.EthBlock;

@Service
public class EmptyBlockPeriod extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(EmptyBlockPeriod.class);

    @Step("Until block <blocknumber>, produced empty blocks should have block periods to be at least <emptyblockperiod>")
    public void waitForEmptyBlockAndCheckTimeSpent(int blocknumber, int emptyblockperiod) {
        while (utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue() < blocknumber) {
            logger.info("sleep a bit, current block #"+utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue()+" expected is #"+blocknumber);
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {}
        }
        QuorumNode node = QuorumNode.Node1;
        int number = blocknumber;
        EthBlock.Block block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        while (number > 0 && (block == null || !block.getTransactions().isEmpty())) {
            number--;
            block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        }
        EthBlock.Block firstEmptyBlock = block;
        int nbEmptyBlock = 0;
        while (number > 0 && block.getTransactions().isEmpty()) {
            nbEmptyBlock++;
            number--;
            block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        }
        BigInteger delta = firstEmptyBlock.getTimestamp().subtract(block.getTimestamp());
        if (nbEmptyBlock > 0) {
            int emptyBlockPeriodSeconds = delta.divide(BigInteger.valueOf(nbEmptyBlock)).intValue();
            if (emptyBlockPeriodSeconds < emptyblockperiod) {
               logger.error("fail to check empty block period %d > %d (%d empty block from #%d)\n", emptyBlockPeriodSeconds, emptyblockperiod, nbEmptyBlock, blocknumber);
            }
            assert(emptyBlockPeriodSeconds >= emptyblockperiod);
        } else {
            logger.warn("not able to check empty block period %d (%d empty block from #%d)\n", emptyblockperiod, nbEmptyBlock, blocknumber);
            assert(false);
        }
    }
}
