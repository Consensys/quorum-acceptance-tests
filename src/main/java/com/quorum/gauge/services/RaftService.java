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

package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import rx.Observable;

import java.util.Arrays;

@Service
public class RaftService extends AbstractService {

    public Observable<RaftAddPeer> addPeer(QuorumNode node, String enode) {
        Request<?, RaftService.RaftAddPeer> request = new Request<>(
                "raft_addPeer",
                Arrays.asList(StringEscapeUtils.unescapeJavaScript(enode)),
                connectionFactory().getWeb3jService(node),
                RaftService.RaftAddPeer.class
        );
        return request.observable().map(raftAddPeer -> {
            raftAddPeer.setNode(node);
            return raftAddPeer;
        });
    }

    public static class RaftAddPeer extends Response<Integer> {
        private QuorumNode node;

        // node that perform addPeer
        public QuorumNode getNode() {
            return node;
        }

        public void setNode(QuorumNode node) {
            this.node = node;
        }
    }
}
