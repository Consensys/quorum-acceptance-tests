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
import com.quorum.gauge.services.DockerInfrastructureService;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is to wait for all docker containers to be running and healthy.
 * <p>
 * Requires DockerWaitMain.properties in the classpath
 */
@SpringBootApplication
@EnableConfigurationProperties
@Profile("dockerwaitmain")
public class DockerWaitMain implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DockerWaitMain.class);

    private static final String PROP_FILE_TEMP = "DockerWaitMain-%s.properties";

    enum WaitType {infra, network}

    private static WaitType waitType;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Missing arg");
        }
        waitType = WaitType.valueOf(args[0]);
        String propFile = String.format(PROP_FILE_TEMP, waitType);
        InputStream is = DockerWaitMain.class.getResourceAsStream("/" + propFile);
        if (is == null) {
            System.out.println("Can't find " + propFile);
            return;
        }
        Properties props = new Properties();
        props.load(is);
        if ("true".equalsIgnoreCase(props.getProperty("wait.disable", "false"))) {
            System.out.println("Template network ... no need to wait");
            return;
        }
        String[] updatedArgs = props.keySet().stream()
            .map(String::valueOf)
            .filter(p -> p.startsWith("quorum."))
            .map(p -> String.format("--%s=%s", p, props.get(p)))
            .toArray(String[]::new);
        new SpringApplicationBuilder(DockerWaitMain.class)
            .properties(props)
            .web(WebApplicationType.NONE)
            .lazyInitialization(true)
            .profiles("dockerwaitmain")
            .run(updatedArgs)
            .close();
    }

    @Autowired(required = false)
    private DockerInfrastructureService dockerService;

    @Autowired
    private QuorumNetworkProperty quorumNetworkProperty;

    @Override
    public void run(String... args) throws Exception {
        switch (waitType) {
            case infra:
                waitForInfra();
                break;
            case network:
                waitForNetwork();
                break;
            default:
                throw new UnsupportedOperationException("wait type not supported");
        }
    }

    private void waitForInfra() throws Exception {
        Observable.just(new AtomicInteger(1))
            .doOnNext(c -> logger.info("Waiting attempt {} for infra to be ready ...", c.getAndIncrement()))
            .flatMap(c -> dockerService.info())
            .retryWhen(o -> o.delay(5, TimeUnit.SECONDS))
            .timeout(5, TimeUnit.MINUTES)
            .blockingSubscribe(i -> {
                logger.info("Infra is ready! {}", i.getName());
            });
    }

    private void waitForNetwork() throws Exception {
        QuorumNetworkProperty.DockerInfrastructureProperty docker = quorumNetworkProperty.getDockerInfrastructure();
        List<Observable<DockerInfrastructureService.BasicContainerState>> all = new ArrayList<>();
        for (String node : docker.getNodes().keySet()) {
            QuorumNetworkProperty.DockerInfrastructureProperty.DockerContainerProperty prop = docker.getNodes().get(node);
            all.add(dockerService.getState(prop.getQuorumContainerId()));
            all.add(dockerService.getState(prop.getTesseraContainerId()));
            if (prop.getEthSignerContainerId().isPresent()) {
                all.add(dockerService.getState(prop.getEthSignerContainerId().get()));
            }
        }
        // check containers wait for 10 minutes for things to start
        int attemptCount = 10;
        int allUp = 1;
        for (int i = 0; i < attemptCount; i++) {
            AtomicReference<DockerInfrastructureService.BasicContainerState> deathState = new AtomicReference<>();
            int status = Observable.zip(all, objects -> {
                boolean isRunning = false;
                for (Object o : objects) {
                    DockerInfrastructureService.BasicContainerState state = (DockerInfrastructureService.BasicContainerState) o;
                    logger.info("{}({}): status = {}, health = {}, dead = {}, ongoing = {}", state.getContainerName(), StringUtils.substring(state.getContainerId(), 0, 12), state.getStatus(), state.getHealthStatus(), state.isDead(), state.isOnGoing());
                    if (state.isDead()) {
                        deathState.set(state);
                        logger.info("{}({}) IS DEAD: status = {}, health = {}, dead = {}, ongoing = {}", state.getContainerName(), StringUtils.substring(state.getContainerId(), 0, 12), state.getStatus(), state.getHealthStatus(), state.isDead(), state.isOnGoing());
                        return -1;
                    }
                    if (state.isOnGoing()) {
                        isRunning = true;
                    }
                    if (state.getHealthStatus().equalsIgnoreCase("unhealthy")) {
                        OutputStreamWriter writer = new OutputStreamWriter(System.err);
                        dockerService.streamLogs(state.getContainerId(), writer);
                        writer.flush();
                        logger.error("Container not healthy: " + state.getContainerName());
                    }
                }
                if (isRunning) {
                    return 1;
                }
                return 0;
            }).blockingFirst();
            if (status == -1) {
                OutputStreamWriter writer = new OutputStreamWriter(System.err);
                dockerService.streamLogs(deathState.get().getContainerId(), writer);
                writer.flush();
                throw new RuntimeException("There's a container not healthy: " + deathState.get().getContainerName());
            }
            if (status == 0) {
                break;
            }

            logger.info("Waiting 60s ... as containers are still starting up");
            Thread.sleep(60000);
            allUp++;
        }
        // grace period to allow network to start up
        if (allUp == 1) {
            logger.info("All good!");
        } else if (allUp < attemptCount) {
            Duration wait = quorumNetworkProperty.getConsensusGracePeriod();
            logger.info("Waiting {}s ... as grace period for network to start", wait.toSeconds());
            Thread.sleep(wait.toMillis());
        } else {
            throw new RuntimeException("Wait timed out!");
        }
        logger.info("Network must be ready!");
    }
}
