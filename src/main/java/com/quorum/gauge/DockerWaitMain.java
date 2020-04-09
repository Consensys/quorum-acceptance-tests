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
import com.quorum.gauge.common.QuorumNetworkProperty.DockerInfrastructureProperty;
import com.quorum.gauge.services.DockerInfrastructureService;
import com.quorum.gauge.services.DockerInfrastructureService.BasicContainerState;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.FileReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.quorum.gauge.common.QuorumNetworkProperty.DockerInfrastructureProperty.DockerContainerProperty;

/**
 * This is to wait for all docker containers to be running and healthy
 */
@SpringBootApplication
@EnableConfigurationProperties
public class DockerWaitMain implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DockerWaitMain.class);

    public static void main(String[] args) throws Exception{
        if (args.length != 1) {
            System.err.println("Missing arg");
            System.exit(1);
        }
        Properties props = new Properties();
        props.load(new FileReader(args[0]));
        if ("true".equalsIgnoreCase(props.getProperty("wait.disable", "false"))) {
            System.out.println("Template network ... no need to wait");
            return;
        }
        new SpringApplicationBuilder(DockerWaitMain.class)
                .properties(props)
                .web(WebApplicationType.NONE)
                .lazyInitialization(true)
                .run(args)
                .close();
    }

    @Autowired(required = false)
    private DockerInfrastructureService dockerService;

    @Autowired
    private QuorumNetworkProperty quorumNetworkProperty;

    @Override
    public void run(String... args) throws Exception {
        DockerInfrastructureProperty docker = quorumNetworkProperty.getDockerInfrastructure();
        List<Observable<BasicContainerState>> all = new ArrayList<>();
        for (String node : docker.getNodes().keySet()) {
            DockerContainerProperty prop = docker.getNodes().get(node);
            all.add(dockerService.getState(prop.getQuorumContainerId()));
            all.add(dockerService.getState(prop.getTesseraContainerId()));
        }
        // check containers
        int attemptCount = 20;
        int allUp = 1;
        for (int i = 0; i < attemptCount; i++) {
            int status = Observable.zip(all, objects -> {
                boolean isRunning = false;
                for (Object o : objects) {
                    BasicContainerState state = (BasicContainerState) o;
                    logger.info("{}: status = {}, health = {}", state.getContainerName(), state.getStatus(), state.getHealthStatus());
                    if (state.isDead()) {
                        return -1;
                    }
                    if (state.isOnGoing()) {
                        isRunning = true;
                    }
                }
                if (isRunning) {
                    return 1;
                }
                return 0;
            }).blockingFirst();
            if (status == -1) {
                throw new RuntimeException("There's a container not healthy");
            }
            if (status == 0) {
                break;
            }
            logger.info("Waiting 3s ... as containers are still starting up");
            Thread.sleep(3000);
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
