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

import org.web3j.protocol.core.Response;

import java.util.LinkedHashMap;

public class NodeInfo extends Response<LinkedHashMap<Object, Object>> {

    public String getEnode() {
        final LinkedHashMap<Object, Object> result = this.getResult();

        if ((result == null) || (result.get("enode") == null)) {
            return null;
        }

        return result.get("enode").toString();
    }
    public String getConsensus() {
        final LinkedHashMap<Object, Object> result = this.getResult();

        if ((result == null) || (result.get("protocols") == null)) {
            return null;
        }
        Object o1 = ((LinkedHashMap)result.get("protocols")).get("eth");
        Object o2 = ((LinkedHashMap)result.get("protocols")).get("istanbul");
        if(o1 != null){
            return ((LinkedHashMap)o1).get("consensus").toString();
        }

        if (o2 != null){
            return ((LinkedHashMap)o2).get("consensus").toString();
        }

        return "";
    }

}
