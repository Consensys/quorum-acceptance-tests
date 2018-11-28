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
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@org.springframework.context.annotation.Configuration
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    @Autowired
    QuorumNetworkProperty networkProperty;

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(5, TimeUnit.MINUTES);
        builder.writeTimeout(5, TimeUnit.MINUTES);
        builder.connectTimeout(5, TimeUnit.MINUTES);
        if (logger.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(logger::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        QuorumNetworkProperty.SocksProxy socksProxyConfig = networkProperty.getSocksProxy();
        if (socksProxyConfig != null) {
            logger.info("Configured SOCKS Proxy ({}:{}) for HTTPClient", socksProxyConfig.getHost(), socksProxyConfig.getPort());
            builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksProxyConfig.getHost(), socksProxyConfig.getPort())));
        }
        return builder.build();
    }
}
