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

    @Step("From block <fromblocknumber> to <toblocknumber>, produced empty blocks should have block periods to be at least <emptyblockperiod>")
    public void waitForEmptyBlockAndCheckTimeSpent(int fromblocknumber, int toblocknumber, int emptyblockperiod) {
        assert(fromblocknumber > toblocknumber);
        while (utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue() < toblocknumber) {
            logger.info("sleep a bit, current block #"+utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue()+" expected is #"+toblocknumber);
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {}
        }
        QuorumNode node = QuorumNode.Node1;

        // Find the first empty block
        int number = toblocknumber;
        EthBlock.Block block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        while (number > fromblocknumber && (block == null || !block.getTransactions().isEmpty())) {
            number--;
            block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        }
        EthBlock.Block firstEmptyBlock = block;

        // Count continuous number of empty blocks
        int nbEmptyBlock = 0;
        while (number > fromblocknumber && block.getTransactions().isEmpty()) {
            nbEmptyBlock++;
            number--;
            block = utilService.getBlockByNumber(node, number).blockingFirst().getBlock();
        }

        // Delta timestamps between two blocks
        BigInteger delta = firstEmptyBlock.getTimestamp().subtract(block.getTimestamp());
        if (nbEmptyBlock > 0) {
            int emptyBlockPeriodSeconds = delta.divide(BigInteger.valueOf(nbEmptyBlock)).intValue();
            if (emptyBlockPeriodSeconds < emptyblockperiod) {
               logger.error("fail to check empty block period "+emptyBlockPeriodSeconds+" > "+emptyblockperiod+" for ("+nbEmptyBlock+" empty blocks from #"+fromblocknumber+" to #"+toblocknumber+")\n");
            }
            assert(emptyBlockPeriodSeconds >= emptyblockperiod);
        } else {
            logger.warn("not able to check empty block period "+emptyblockperiod+" ("+nbEmptyBlock+" empty block from #"+fromblocknumber+" to #"+toblocknumber+")\n");
            assert(false);
        }
    }
}
