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

package com.quorum.gauge.common;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Construct a string containing geth args
 */
public class GethArgBuilder {
    private Map<String, String> args = new HashMap<>();

    private GethArgBuilder() {

    }

    public static GethArgBuilder newBuilder() {
        return new GethArgBuilder();
    }

    public GethArgBuilder overrideWith(GethArgBuilder anotherBuilder) {
        for (Map.Entry<String, String> entry : anotherBuilder.args.entrySet()) {
            args.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String argName : args.keySet()) {
            builder.append(argName).append(" ").append(args.get(argName)).append(" ");
        }
        return builder.toString();
    }

    /**
     * Add/remove {@code --permissioned}
     * @return
     */
    public GethArgBuilder permissioned(boolean yes) {
        if (yes) {
            args.put("--permissioned", "");
        } else {
            args.remove("--permissioned");
        }
        return this;
    }

    /**
     * Add/remove {@code --raftdnsenable}
     * @return
     */
    public GethArgBuilder raftdnsenable(boolean yes) {
        if (yes) {
            args.put("--raftdnsenable", "");
        } else {
            args.remove("--raftdnsenable");
        }
        return this;
    }

    /**
     * Add/remove {@code --gcmode}
     * @param mode blank means remove
     * @return
     */
    public GethArgBuilder gcmode(String mode) {
        if (StringUtils.isBlank(mode)) {
            args.remove("--gcmode");
        } else {
            args.put("--gcmode", mode);
        }
        return this;
    }

    /**
     * Add/remove {@code --raftjoinexisting}
     * @param raftId null means remove
     * @return
     */
    public GethArgBuilder raftjoinexisting(Integer raftId) {
        if (raftId == null) {
            args.remove("--raftjoinexisting");
        } else {
            args.put("--raftjoinexisting", String.valueOf(raftId));
        }
        return this;
    }

    /**
     * Add/remove {@code --allow-insecure-unlock}
     * @since Geth 1.9.7
     * @return
     */
    public GethArgBuilder allowInsecureUnlock(boolean yes) {
        if (yes) {
            args.put("--allow-insecure-unlock", "");
        } else {
            args.remove("--allow-insecure-unlock");
        }
        return this;
    }

    public GethArgBuilder privacyMarkerEnable(boolean yes) {
        if (yes) {
            args.put("--privacymarker.enable", "");
        } else {
            args.remove("--privacymarker.enable");
        }
        return this;
    }
}
