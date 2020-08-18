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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * web3j 4.5.x introduces BatchRequest but there's a huge break when we upgrade
 * So we create our own here.
 */
@JsonIgnoreProperties({"jsonrpc", "id", "method", "params"})
@JsonSerialize(using = BatchRequest.Serializer.class)
public class BatchRequest extends Request<Object, BatchResponse> {
    @JsonMerge
    private final List<Request<?, ObjectResponse>> requests;

    public BatchRequest(Web3jService client, List<Request<?, ObjectResponse>> requests) {
        super("", Collections.emptyList(), client, BatchResponse.class);
        this.requests = requests;
    }

    public List<Request<?, ObjectResponse>> getRequests() {
        return requests;
    }

    public static class Collector {
        private List<Request<?, ObjectResponse>> requests;
        private Collector() {
            this.requests = new ArrayList<>();
        }
        public static Collector create() {
            return new Collector();
        }
        public Collector add(String method, List<Object> params) {
            requests.add(new Request<>(method, params, null, ObjectResponse.class));
            return this;
        }
        public List<Request<?, ObjectResponse>> toList() {
            return requests;
        }
        public int size() { return requests.size(); }

        public Request getByID(long id) {
            return requests.stream().filter(r -> r.getId() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("no such ID"));
        }
    }

    public static class Serializer extends JsonSerializer<BatchRequest> {
        @Override
        public void serialize(BatchRequest value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (Request r : value.getRequests()) {
                gen.writeObject(r);
            }
            gen.writeEndArray();
        }
    }
}
