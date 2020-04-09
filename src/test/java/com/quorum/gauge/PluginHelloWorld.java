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

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.StringResponse;
import com.thoughtworks.gauge.Step;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@Service
public class PluginHelloWorld extends AbstractSpecImplementation {

    @Step("Calling `plugin@helloworld_greeting` API in <node> with single parameter <name> must return <expected>")
    public void greeting(QuorumNetworkProperty.Node node, String name, String expected) {
        StringResponse res = rpcService.call(node, "plugin@helloworld_greeting", Collections.singletonList(name), StringResponse.class)
                .blockingFirst();

        Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
        assertThat(err.getMessage()).as("plugin@helloworld_greeting must succeed").isBlank();
        assertThat(res.getResult()).isEqualTo(expected);
    }
}