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


import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.services.SocksProxyEmbeddedServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@org.springframework.context.annotation.Configuration
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public Configuration() {
        // this is to set the timeout when using Schedulers.io()
        // default is only 60 seconds so we bump to higher value
        System.setProperty("rx2.io-keep-alive-time", String.valueOf(TimeUnit.MINUTES.toSeconds(5)));
    }

    @Autowired
    QuorumNetworkProperty networkProperty;

    public static OkHttpClient.Builder cloneBuilderFromClient(OkHttpClient client){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        configureTimeoutsAndSSL(builder);

        client.interceptors().forEach(builder::addInterceptor);

        if (client.proxy() != null){
            builder.proxy(client.proxy());
        }
        return builder;
    }

    private static void configureTimeoutsAndSSL(OkHttpClient.Builder builder){
        builder.readTimeout(10, TimeUnit.MINUTES);
        builder.writeTimeout(10, TimeUnit.MINUTES);
        builder.connectTimeout(10, TimeUnit.MINUTES);
        builder.callTimeout(1, TimeUnit.MINUTES);

        // configure to ignore SSL
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public OkHttpClient okHttpClient(Optional<SocksProxyEmbeddedServer> socksProxyEmbeddedServer) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        configureTimeoutsAndSSL(builder);

        if (networkProperty.getOauth2Server() != null) {
            builder.addInterceptor(chain -> {
                // TODO - event polling is done in various threads spawned by the the web3j library itself so I don't think thread local for auth is good enough
                String token = Context.retrieveAccessToken();
                if (StringUtils.isEmpty(token)) {
                    return chain.proceed(chain.request());
                }
                Request request = chain.request().newBuilder()
                        .addHeader("Authorization", token)
                        .build();
                return chain.proceed(request);
            });
        }
        builder.addInterceptor(chain -> {
            String psi = Context.retrievePSI();
            if (StringUtils.isEmpty(psi)) {
                return chain.proceed(chain.request());
            }
            Request request = chain.request().newBuilder()
                .addHeader("Quorum-PSI", psi)
                .build();
            return chain.proceed(request);
        });
        Logger httpLogger = LoggerFactory.getLogger(Configuration.class.getPackageName() + ".HttpLogger");
        if (httpLogger.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(httpLogger::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        QuorumNetworkProperty.SocksProxy socksProxyConfig = networkProperty.getSocksProxy();
        if (socksProxyConfig != null) {
            InetSocketAddress address = null;
            if (socksProxyEmbeddedServer.isPresent()) {
                if (socksProxyEmbeddedServer.get().isStarted()) {
                    address = new InetSocketAddress(socksProxyEmbeddedServer.get().getListenerAddress(), socksProxyEmbeddedServer.get().getListenerPort());
                } else {
                    logger.warn("SocksProxy tunneling is configured but not started");
                }
            } else {
                address = new InetSocketAddress(socksProxyConfig.getHost(), socksProxyConfig.getPort());
            }
            if (address != null) {
                logger.info("Configured SOCKS Proxy ({}) for HTTPClient", address);
                builder.proxy(new Proxy(Proxy.Type.SOCKS, address));
            }
        }

        return builder.build();
    }

}
