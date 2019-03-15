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
import com.thoughtworks.gauge.Step;
import org.springframework.stereotype.Service;

@Service
public class PrivateStateValidation extends AbstractSpecImplementation {

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deploySimpleContract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Deploy a <flag> contract `C1` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC1Contract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Deploy a <flag> contract `C2` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC2Contract(ContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
    }

    @Step("Fail to execute <contractName>'s `get()` function in <node>")
    public void failGetExecution(String contractName, QuorumNode node) {
    }

    @Step("Fail to execute <contractName>'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void failSetExecution(String contractName, QuorumNode node, String privateFor) {
        String[] participants = privateFor.split(",");
    }

    @Step("Fire and forget execution of <contractName>'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForget(String contractName, QuorumNode node, String privateFor) {
        String[] participants = privateFor.split(",");
    }
}
