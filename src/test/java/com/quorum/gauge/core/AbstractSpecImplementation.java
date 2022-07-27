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

package com.quorum.gauge.core;

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.services.*;
import com.thoughtworks.gauge.datastore.DataStore;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSpecImplementation.class);

    @Autowired
    protected ContractService contractService;

    @Autowired
    protected StorageMasterService storageMasterService;

    @Autowired
    protected AccumulatorService accumulatorService;

    @Autowired
    protected AdminService adminService;

    @Autowired
    protected DebugService debugService;

    @Autowired
    protected NestedContractService nestedContractService;

    @Autowired
    protected PermissionsContractService permissionContractService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected RawContractService rawContractService;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected PrivacyMarkerTransactionService privacyMarkerTransactionService;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected PrivacyService privacyService;

    @Autowired
    protected OkHttpClient okHttpClient;

    @Autowired
    protected UtilService utilService;

    @Autowired
    protected QuorumNetworkProperty networkProperty;

    @Autowired
    protected RPCService rpcService;

    @Autowired
    protected GraphQLService graphQLService;

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Autowired
    protected HashicorpVaultAccountCreationService hashicorpVaultAccountCreationService;

    @Autowired
    protected HashicorpVaultSigningService hashicorpVaultSigningService;

    @Autowired
    protected ContractCodeReaderService contractCodeReaderService;

    protected BigInteger currentBlockNumber() {
        return mustHaveValue(DataStoreFactory.getScenarioDataStore(), "blocknumber", BigInteger.class);
    }

    protected void saveCurrentBlockNumber() {
        BigInteger blockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
        DataStoreFactory.getScenarioDataStore().put("blocknumber", blockNumber);
    }

    protected <T> T mustHaveValue(DataStore ds, String key, Class<T> clazz) {
        Object v = ds.get(key);
        assertThat(v).as("Value class for key [" + key + "] in Gauge DataStore").isInstanceOf(clazz);
        assertThat(v).as("Value for key [" + key + "] in Gauge DataStore").isNotNull();
        return (T) v;
    }

    protected <T> Optional<T> haveValue(DataStore ds, String key, Class<T> clazz) {
        Object v = ds.get(key);
        if (Objects.isNull(v)) {
            return Optional.empty();
        } else {
            return Optional.of((T) v);
        }
    }

    /**
     * Check in all Gauge Data Stores
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    protected <T> T mustHaveValue(String key, Class<T> clazz) {
        Object v = null;
        boolean isInstance = false;
        for (DataStore ds : new DataStore[]{DataStoreFactory.getScenarioDataStore(), DataStoreFactory.getSpecDataStore(), DataStoreFactory.getSuiteDataStore()}) {
            v = ds.get(key);
            if (v != null && clazz.isInstance(v)) {
                isInstance = true;
                break;
            }
        }
        assertThat(v).as("Value for key [" + key + "] in all Gauge DataStores").isNotNull();
        assertThat(isInstance).as("Value for key [" + key + "] of type [" + clazz.getName() + "] in all Gauge DataStores").isTrue();
        return (T) v;
    }

    protected <T> T haveValue(DataStore ds, String key, Class<T> clazz, T defaultValue) {
        Object v = ds.get(key);
        if (v == null) {
            return defaultValue;
        }
        return mustHaveValue(ds, key, clazz);
    }

    protected void waitForBlockHeight(int currentBlockHeight, int untilBlockHeight) {
        AtomicInteger lastBlockHeight = new AtomicInteger(currentBlockHeight);
        utilService.getCurrentBlockNumber().flatMap(ethBlockNumber -> {
            if (ethBlockNumber.getBlockNumber().intValue() < untilBlockHeight) {
                logger.debug("Current block height is {}, wait until {}", ethBlockNumber.getBlockNumber(), untilBlockHeight);
                lastBlockHeight.set(ethBlockNumber.getBlockNumber().intValue());
                throw new RuntimeException("let's wait and retry");
            }
            return Observable.just(true);
        }).retryWhen(
                attempts -> attempts.zipWith(Observable.range(1, untilBlockHeight), (total, i) -> i)
                        .flatMap(i -> Observable.timer(3, TimeUnit.SECONDS))
                        .doOnComplete(() -> {
                            throw new RuntimeException("Timed out! Can't wait until block height is " + untilBlockHeight + " higher. Last block height was " + lastBlockHeight.get());
                        })
        ).blockingFirst();
    }

    // created a fixed thread pool executor and inject ThreadLocal values into the scheduled thread
    protected Scheduler threadLocalDelegateScheduler(int threadCount) {
        QuorumNodeConnectionFactory connectionFactory = Context.getConnectionFactory();
        String accessToken = Context.retrieveAccessToken();
        Executor executor = Executors.newFixedThreadPool(Math.min(threadCount, 100), new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "RxJavaCustom-" + (count++)) {
                    @Override
                    public void run() {
                        Context.setConnectionFactory(connectionFactory);
                        Context.storeAccessToken(accessToken);
                        super.run();
                    }
                };
            }
        });
        return Schedulers.from(executor);
    }

    /**
     * @return number of nodes specified in the configuration yml
     */
    protected int numberOfQuorumNodes() {
        return networkProperty.getNodes().size();
    }

    public enum Status {
        successfully, unsuccessfully
    }
}
