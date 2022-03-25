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

import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Throwing {@link UnsupportedOperationException} for all implementation.
 * This bean is created only if there is no {@link InfrastructureService} implementation being available
 */
@Service
@ConditionalOnMissingBean(DockerInfrastructureService.class)
public class UnsupportedInfrastructureService implements InfrastructureService, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(UnsupportedInfrastructureService.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("No infrastructure service is used");
    }

    @Override
    public Observable<Boolean> startNode(NodeAttributes attributes, ResourceCreationCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> deleteResources(List<String> resourceIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> deleteNetwork(NetworkResources networkResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Integer> checkNetwork(NetworkResources existingNetworkResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<String> modifyFile(String resourceId, String filePath, FileContentModifier modifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<String> writeFile(String resourceId, String filePath, String fileContent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> isGeth(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<String> getName(final String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> isBesu(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> deleteDatadirs(NetworkResources networkResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> stopResource(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> startResource(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> restartResource(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> wait(String resourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<Boolean> grepLog(String resourceId, String grepStr, long timeoutAmount, TimeUnit timeoutUnit) {
        throw new UnsupportedOperationException();
    }
}
