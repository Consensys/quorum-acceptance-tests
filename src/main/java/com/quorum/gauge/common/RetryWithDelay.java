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

import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * Retry an {@link Observable} with configurable retry limit and timeout
 * between attempts
 */
public class RetryWithDelay implements Func1<Observable<? extends Throwable>, Observable<?>> {

    private final int maxRetries;

    private final int retryDelayMillis;

    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> call(final Observable<? extends Throwable> attempts) {
        return attempts
            .flatMap(throwable -> {
                if (++retryCount < maxRetries) {
                    // When this Observable calls onNext, the original
                    // Observable will be retried
                    return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                }

                // Max retries hit, just pass the error along.
                return Observable.error(new RuntimeException("retry timed out"));
            });
    }

}
