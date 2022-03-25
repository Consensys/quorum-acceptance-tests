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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.collect.ImmutableMap;
import com.quorum.gauge.common.GethArgBuilder;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNetworkProperty.DockerInfrastructureProperty.DockerContainerProperty;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Using Docker to manage the Quorum Network infrastructure if quorum.docker-infrastructure.enabled is true
 */
@Service
@ConditionalOnProperty(prefix = "quorum", name = "docker-infrastructure.enabled", havingValue = "true")
public class DockerInfrastructureService
    extends AbstractService
    implements InfrastructureService, InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(DockerInfrastructureService.class);

    private Map<String, QuorumImageConfig> quorumDockerImageCatalog = new HashMap<>();
    private Map<String, String> tesseraDockerImageCatalog = new HashMap<>();
    private QuorumNetworkProperty.DockerInfrastructureProperty infraProperty;
    private DockerClient dockerClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        if (StringUtils.isNotBlank(networkProperty().getDockerInfrastructure().getHost())) {
            configBuilder.withDockerHost(networkProperty().getDockerInfrastructure().getHost());
        }
        DefaultDockerClientConfig config = configBuilder.build();
        infraProperty = networkProperty().getDockerInfrastructure();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        quorumDockerImageCatalog = ImmutableMap.<String, QuorumImageConfig>builder()
            .put("develop", new QuorumImageConfig(Optional.ofNullable(infraProperty.getTargetQuorumImage()).orElse("quorumengineering/quorum:develop"), GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .put("latest", new QuorumImageConfig("quorumengineering/quorum:latest", GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .put("v2.5.0", new QuorumImageConfig("quorumengineering/quorum:2.5.0", GethArgBuilder.newBuilder()))
            .put("21.1.0", new QuorumImageConfig("quorumengineering/quorum:21.1.0", GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .put("21.4.0", new QuorumImageConfig("quorumengineering/quorum:21.4.0", GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .put("21.10.0", new QuorumImageConfig("quorumengineering/quorum:21.10.0", GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .put("2.7.0", new QuorumImageConfig("quorumengineering/quorum:2.7.0", GethArgBuilder.newBuilder().allowInsecureUnlock(true)))
            .build();
        tesseraDockerImageCatalog = ImmutableMap.of(
            "develop", Optional.ofNullable(infraProperty.getTargetTesseraImage()).orElse("quorumengineering/tessera:develop"),
            "latest", "quorumengineering/tessera:latest",
            "0.10.5", "quorumengineering/tessera:0.10.5",
            "21.1.0", "quorumengineering/tessera:21.1.0",
            "21.10.0", "quorumengineering/tessera:21.10.0"
        );
    }

    /**
     * Use existing containers as templates to populate the same config before staring them up
     * with the provided Docker images
     *
     * @param attributes
     * @param callback
     * @return
     */
    @Override
    public Observable<Boolean> startNode(NodeAttributes attributes, ResourceCreationCallback callback) {
        DockerContainerProperty p = infraProperty.getNodes().get(attributes.getNode());
        String quorumImage = "";
        if (quorumDockerImageCatalog.containsKey(attributes.getQuorumVersionKey())) {
            QuorumImageConfig quorumImageConfig = quorumDockerImageCatalog.get(attributes.getQuorumVersionKey());
            attributes.withAdditionalGethArgs(attributes.getAdditionalGethArgsBuilder().overrideWith(quorumImageConfig.getArgBuilder()));
            quorumImage = quorumImageConfig.getImage();
        }
        String tesseraImage = tesseraDockerImageCatalog.getOrDefault(attributes.getTesseraVersionKey(), "");
        return Observable.zip(
            startContainerFromTemplate(p.getQuorumContainerId(), attributes, quorumImage, callback).subscribeOn(Schedulers.io()),
            startContainerFromTemplate(p.getTesseraContainerId(), attributes, tesseraImage, callback).subscribeOn(Schedulers.io()),
            (q, t) -> q && t);
    }

    @Override
    public Observable<Boolean> deleteResources(List<String> resourceIds) {
        return Observable.fromIterable(resourceIds)
            .doOnNext(id -> logger.debug("Deleting container {}", StringUtils.substring(id, 0, 12)))
            .map(id -> {
                try {
                    dockerClient.stopContainerCmd(id).exec();
                } catch (NotModifiedException e) {
                    logger.debug("container {} already stopped", id);
                }
                dockerClient.removeContainerCmd(id).exec();
                return true;
            });
    }

    @Override
    public Observable<Boolean> deleteNetwork(NetworkResources networkResources) {
        if (CollectionUtils.isEmpty(networkResources)) {
            return Observable.just(true);
        }
        return Observable.zip(
            Observable.fromIterable(networkResources.getNodeNames())
                .doOnNext(node -> logger.debug("Cleaning up resources for node {}, resources={}", node, networkResources.get(node)))
                .map(networkResources::get)
                .map(r -> deleteResources(r).subscribeOn(Schedulers.io()))
                .toList().blockingGet()
            , oks -> true);
    }

    @Override
    public Observable<Integer> checkNetwork(NetworkResources networkResources) {
        return Observable.zip(Observable.fromIterable(networkResources.keySet())
                .map(networkResources::get)
                .flatMapIterable(ids -> ids)
                .map(id ->
                    Observable.just(dockerClient.inspectContainerCmd(id).exec())
                        .map(res -> {
                            int status = STATUS_RUNNING | STATUS_HEALTHY;
                            if (!StringUtils.equalsIgnoreCase("running", res.getState().getStatus())) {
                                status = status ^ STATUS_RUNNING;
                            }
                            if (res.getState().getHealth() == null || !StringUtils.equalsIgnoreCase("healthy", res.getState().getHealth().getStatus())) {
                                status = status ^ STATUS_HEALTHY;
                            }
                            logger.debug("Container {}: status={}", res.getName(), status);
                            return status;
                        })
                        .subscribeOn(Schedulers.io()))
                .toList().blockingGet()
            , statuses -> Arrays.stream(statuses).mapToInt(s -> (int) s).reduce(STATUS_RUNNING | STATUS_HEALTHY, (a, b) -> a & b)
        );
    }


    @Override
    public Observable<String> writeFile(String resourceId, String filePath, String fileContent) {
        return Observable.just(fileContent).map(data -> {
            InputStream is = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TarArchiveOutputStream taos = new TarArchiveOutputStream(baos);
            TarArchiveEntry entry = new TarArchiveEntry(new File(filePath).getName());
            entry.setSize(data.length());
            entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
            taos.putArchiveEntry(entry);
            IOUtils.copy(new ByteArrayInputStream(data.getBytes()), taos);
            taos.closeArchiveEntry();
            taos.finish();
            taos.close();
            baos.close();
            is = new ByteArrayInputStream(baos.toByteArray());
            return is;
        }).map(inputStr -> {
            dockerClient.copyArchiveToContainerCmd(resourceId)
                .withTarInputStream(inputStr)
                .withRemotePath(new File(filePath).getParent())
                .exec();
            return resourceId;
        });
    }

    /**
     * Read content of the give file, call the modifier to get the new content and write back to the file
     *
     * @param resourceId containerId
     * @param filePath   internal container path
     * @param modifier
     * @return
     */
    @Override
    public Observable<String> modifyFile(String resourceId, String filePath, FileContentModifier modifier) {
        return Observable.just(dockerClient.copyArchiveFromContainerCmd(resourceId, filePath).exec())
            .map(is -> {
                StringWriter writer = new StringWriter();
                TarArchiveInputStream tis = new TarArchiveInputStream(is);
                TarArchiveEntry tarEntry = null;
                while ((tarEntry = tis.getNextTarEntry()) != null) {
                    if (tarEntry.isDirectory()) {
                        throw new RuntimeException("Expect a file not a directory");
                    } else {
                        IOUtils.copy(new InputStreamReader(tis), writer);
                    }
                }
                tis.close();
                return writer.toString();
            })
            .map(modifier::modify)
            .map(str -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                TarArchiveOutputStream taos = new TarArchiveOutputStream(baos);
                TarArchiveEntry entry = new TarArchiveEntry(new File(filePath).getName());
                entry.setSize(str.length());
                entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
                taos.putArchiveEntry(entry);
                IOUtils.copy(new ByteArrayInputStream(str.getBytes()), taos);
                taos.closeArchiveEntry();
                taos.finish();
                taos.close();
                baos.close();
                return new ByteArrayInputStream(baos.toByteArray());
            })
            .map(is -> {
                dockerClient.copyArchiveToContainerCmd(resourceId)
                    .withTarInputStream(is)
                    .withRemotePath(new File(filePath).getParent())
                    .exec();
                return resourceId;
            });
    }

    @Override
    public Observable<Boolean> isGeth(String resourceId) {
        return Observable.just(dockerClient.inspectContainerCmd(resourceId).exec())
            .map(InspectContainerResponse::getConfig)
            .map(ContainerConfig::getLabels)
            .map(l -> l.containsKey("QuorumContainer"));
    }

    @Override
    public Observable<String> getName(String resourceId) {
        return Observable.just(dockerClient.inspectContainerCmd(resourceId).exec())
            .map(InspectContainerResponse::getName);
    }

    @Override
    public Observable<Boolean> isBesu(String resourceId) {
        return Observable.just(dockerClient.inspectContainerCmd(resourceId).exec())
            .map(InspectContainerResponse::getConfig)
            .map(ContainerConfig::getLabels)
            .map(l -> l.containsKey("BesuContainer"));
    }

    /**
     * Datadir in docker network is {@code /data/qdata}
     *
     * @param networkResources
     * @return
     */
    @Override
    public Observable<Boolean> deleteDatadirs(NetworkResources networkResources) {
        if (CollectionUtils.isEmpty(networkResources)) {
            return Observable.just(true);
        }
        return Observable.fromIterable(networkResources.allResourceIds())
            .filter(containerId -> this.isGeth(containerId).blockingFirst())
            .doOnNext(id -> logger.debug("Deleting datadir in container {}", StringUtils.substring(id, 0, 12)))
            .map(containerId -> dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", "rm -rf /data/qdata && rm -rf /data/tm")
                .exec())
            .map(execCreateCmdResponse -> dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .withDetach(true)
                .exec(new ResultCallback.Adapter<>())
                .awaitCompletion())
            .map(x -> true)
            .doOnComplete(() -> {
                logger.debug("Datadir deletion completed");
            });
    }

    @Override
    public Observable<Boolean> stopResource(String resourceId) {
        return Observable.just(resourceId)
            .doOnNext(id -> logger.debug("Stopping container {}", StringUtils.substring(id, 0, 12)))
            .map(id -> {
                dockerClient.stopContainerCmd(id).exec();
                return true;
            });
    }

    @Override
    public Observable<Boolean> startResource(String resourceId) {
        return Observable.just(resourceId)
            .doOnNext(id -> logger.debug("Starting container {}", StringUtils.substring(id, 0, 12)))
            .map(id -> {
                dockerClient.startContainerCmd(id).exec();
                return true;
            });
    }

    @Override
    public Observable<Boolean> restartResource(String resourceId) {
        return Observable.just(resourceId)
            .doOnNext(id -> logger.debug("Restarting container {}", StringUtils.substring(id, 0, 12)))
            .map(id -> {
                dockerClient.restartContainerCmd(id).exec();
                return true;
            });
    }

    /**
     * Wait for container to be in "healthy" status
     * Timeout is 3 * 30 seconds
     *
     * @param resourceId containder Id
     * @return true if container is in "healthy" status
     */
    @Override
    public Observable<Boolean> wait(String resourceId) {
        return Observable.just(resourceId)
            .map(id -> {
                for (int i = 1; i <= 30; i++) {
                    BasicContainerState state = new BasicContainerState(dockerClient.inspectContainerCmd(id).exec());
                    logger.debug("Waiting attempt {} for container {}({}), status = {}, health = {}", i, state.getContainerName(), StringUtils.substring(state.getContainerId(), 0, 12), state.getStatus(), state.getHealthStatus());
                    if (state.isDead()) {
                        return false;
                    } else if (!state.isOnGoing()) {
                        return true;
                    } else {
                        Thread.sleep(3000);
                    }
                }
                return false;
            });
    }

    @Override
    public Observable<Boolean> grepLog(String resourceId, String grepStr, long timeoutAmount, TimeUnit timeoutUnit) {
        Pattern regex = Pattern.compile(grepStr);
        return Observable.just(resourceId)
            .map(id -> dockerClient.logContainerCmd(id)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ResultCallback.Adapter<>() {
                    // TODO this should probably be replaced with some Observable wrapper that tracks an externally defined flag
                    // I think onComplete is automatically called when the container is shut down
                    // which causes awaitCompletion to return true - which in turn causes a false positive
                    // pattern match result
                    boolean patternFound = false;

                    @Override
                    public void onNext(Frame object) {
                        if (regex.matcher(object.toString()).find()) {
                            patternFound = true;
                            onComplete();
                        }
                    }

                    @Override
                    public boolean awaitCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException {
                        // mix complete success with whether the pattern was found
                        return super.awaitCompletion(timeout, timeUnit) && patternFound;
                    }
                })
                .awaitCompletion(timeoutAmount, timeoutUnit)
            );
    }

    private Observable<Boolean> startContainerFromTemplate(String templateContainerId, NodeAttributes attr, String image, ResourceCreationCallback callback) {
        return Observable.just(templateContainerId)
            .map(id -> {
                InspectContainerResponse res = dockerClient.inspectContainerCmd(id).exec();
                if (!StringUtils.equalsIgnoreCase("created", res.getState().getStatus())) {
                    throw new IllegalStateException("Container " + res.getName() + " status must be 'created' in order to be a template");
                }
                Map<String, ContainerNetwork> networks = res.getNetworkSettings().getNetworks();
                String networkName = networks.keySet().iterator().next();
                String ip = networks.get(networkName).getIpamConfig().getIpv4Address();
                List<String> aliases = networks.get(networkName).getAliases();
                HostConfig hostConfig = res.getHostConfig();
                hostConfig.withNetworkMode(networkName); // Careful, this value is used to populate NetworkConfig in CreateContainerCmdImpl
                List<String> env = new ArrayList<>();
                if (attr.isStartFresh()) {
                    // Docker endpoint will check this variable
                    // if true means current datadir will be completely
                    // replace by original datadir
                    env.add("ALWAYS_REFRESH=true");
                }
                if (StringUtils.isNotBlank(attr.getAdditionalGethArgs())) {
                    env.add("ADDITIONAL_GETH_ARGS=" + attr.getAdditionalGethArgs());
                }
                if (res.getConfig().getEnv() != null) {
                    env.addAll(Arrays.stream(res.getConfig().getEnv()).collect(Collectors.toList()));
                }
                String containerImage = image;
                if (StringUtils.isBlank(image)) {
                    containerImage = res.getConfig().getImage();
                }
                Map<String, String> labels = res.getConfig().getLabels();
                labels.put("ClonedFromContainerId", templateContainerId);
                labels.put("ClonedFromContainerName", res.getName());
                CreateContainerResponse cRes = dockerClient.createContainerCmd(containerImage)
                    .withName(res.getName() + "-clone")
                    .withHostName(res.getConfig().getHostName())
                    .withDomainName(res.getConfig().getDomainName())
                    .withExposedPorts(res.getConfig().getExposedPorts())
                    .withEnv(env)
                    .withHealthcheck(res.getConfig().getHealthcheck())
                    .withEntrypoint(res.getConfig().getEntrypoint())
                    .withHostConfig(hostConfig)
                    .withIpv4Address(ip)
                    .withAliases(aliases)
                    .withLabels(labels)
                    .exec();
                String newContainerId = cRes.getId();
                logger.debug("Created container {}", StringUtils.substring(newContainerId, 0, 12));
                dockerClient.startContainerCmd(newContainerId).exec();
                callback.onCreate(newContainerId);
                logger.debug("Started container {}", StringUtils.substring(newContainerId, 0, 12));
                return newContainerId;
            })
            .map(id -> this.wait(id).blockingFirst());
    }

    public Observable<BasicContainerState> getState(String containerId) {
        return Observable.just(containerId)
            .map(id -> dockerClient.inspectContainerCmd(id).exec())
            .map(s -> new BasicContainerState(containerId, s.getName(), s.getState().getStatus(), s.getState().getHealth().getStatus()));
    }

    public Observable<Info> info() {
        return Observable.just(dockerClient.infoCmd().exec());
    }

    /**
     * @param containerId
     * @param writer      caller must call close()
     * @throws InterruptedException
     */
    public void streamLogs(String containerId, Writer writer) throws InterruptedException {
        dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withTailAll()
            .withFollowStream(false)
            .exec(new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame object) {
                    try {
                        writer.write(object.toString() + "\n");
                    } catch (IOException e) {
                        onError(e);
                    }
                }
            }).awaitCompletion();
    }

    @Override
    public void destroy() throws Exception {
        dockerClient.close();
    }

    static class QuorumImageConfig {
        private String image;
        private GethArgBuilder argBuilder;

        QuorumImageConfig(String image, GethArgBuilder argBuilder) {
            this.image = image;
            this.argBuilder = argBuilder;
        }

        public String getImage() {
            return image;
        }

        public GethArgBuilder getArgBuilder() {
            return argBuilder;
        }

    }

    public static class BasicContainerState {
        private String containerId;
        private String containerName;
        private String status;
        private String healthStatus;

        public BasicContainerState() {
        }

        public BasicContainerState(InspectContainerResponse res) {
            this(
                res.getId(),
                res.getName(),
                res.getState().getStatus(),
                Optional.ofNullable(res.getState().getHealth()).orElse(new HealthState()).getStatus()
            );
        }

        public BasicContainerState(String containerId, String containerName, String status, String healthStatus) {
            this.containerId = containerId;
            this.containerName = containerName;
            this.status = status;
            this.healthStatus = healthStatus;
        }

        public String getContainerId() {
            return containerId;
        }

        public String getStatus() {
            return status;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public String getContainerName() {
            return containerName;
        }

        /**
         * Container is expected to have health check and only up when health status is `healthy`.
         * In some occasions, container healtch check may produce unhealthy but the container is still starting up.
         *
         * @return true if the container is still starting up and false otherwise
         */
        public boolean isOnGoing() {
            return !StringUtils.equalsIgnoreCase("healthy", healthStatus);
        }

        /**
         * @return true if container is not running
         */
        public boolean isDead() {
            return StringUtils.equalsIgnoreCase("dead", status) || StringUtils.equalsIgnoreCase("exited", status);
        }
    }
}
