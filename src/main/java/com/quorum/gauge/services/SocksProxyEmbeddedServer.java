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

import com.quorum.gauge.common.QuorumNetworkProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.auth.UserAuthMethodFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mimic:
 * <p>
 * {@code ssh -D 5000 -N -o ServerAliveInterval=30 -i <key> <user>@<host> }
 */
@Service
@ConditionalOnProperty(prefix = "quorum", name = "socks-proxy.tunnel.enabled", havingValue = "true")
public class SocksProxyEmbeddedServer implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(SocksProxyEmbeddedServer.class);
    @Autowired
    QuorumNetworkProperty networkProperty;
    QuorumNetworkProperty.SocksProxy.SSHTunneling config;
    private String listenerAddress;
    private int listenerPort;
    private SshClient client;
    private ClientSession session;
    private AtomicBoolean started;

    @Override
    public void afterPropertiesSet() throws Exception {
        config = networkProperty.getSocksProxy().getTunnel();
        client = SshClient.setUpDefaultClient();
        client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        CoreModuleProperties.HEARTBEAT_INTERVAL.set(client, Duration.of(30, ChronoUnit.SECONDS));
        CoreModuleProperties.HEARTBEAT_REPLY_WAIT.set(client, Duration.of(3, ChronoUnit.SECONDS));
        client.setKeyIdentityProvider(new FileKeyPairProvider(Paths.get(resolveTilde(config.getPrivateKeyFile()))));
        client.setUserAuthFactoriesNames(UserAuthMethodFactory.PUBLIC_KEY);
        client.start();
        started = new AtomicBoolean(false);
        if (config.isAutoStart()) {
            start();
        }
    }

    public void start() throws Exception {
        if (started.get()) {
            return;
        }
        session = client.connect(config.getUser(), config.getHost(), 22).verify(10, TimeUnit.SECONDS).getSession();
        session.auth().verify(10, TimeUnit.SECONDS);
        SshdSocketAddress address = session.startDynamicPortForwarding(new SshdSocketAddress("localhost", 0));
        logger.info("Socks Proxy Embedded Server started on {}", address);
        listenerAddress = address.getHostName();
        listenerPort = address.getPort();
        started.set(true);
    }

    public boolean isStarted() {
        return started.get();
    }

    @Override
    public void destroy() throws Exception {
        if (session != null) {
            session.close();
        }
        if (client != null) {
            client.close();
        }
    }

    private String resolveTilde(String f) {
        assert StringUtils.isNotBlank(f);
        if (f.startsWith("~" + File.separator)) {
            return FileUtils.getUserDirectoryPath() + f.substring(1);
        } else if (f.startsWith("~")) {
            throw new UnsupportedOperationException("can't expand home dir. Missing path separator after ~?");
        }
        return f;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public String getListenerAddress() {
        return listenerAddress;
    }
}
