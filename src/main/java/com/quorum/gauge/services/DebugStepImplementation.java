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

import com.thoughtworks.gauge.Step;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logging all methods implementing Gauge Step
 */
@Aspect
@Service
public class DebugStepImplementation {
    private static final Logger logger = LoggerFactory.getLogger(DebugStepImplementation.class);

    @Around("@annotation(step)")
    public Object logging(ProceedingJoinPoint jp, Step step) throws Throwable {
        if (logger.isDebugEnabled()) {
            StringBuilder argStr = new StringBuilder();
            for (Object arg : jp.getArgs()) {
                argStr.append("|").append(arg);
            }
            if (argStr.length() > 0) {
                argStr.deleteCharAt(0);
            }
            String clazzName = jp.getSignature().getDeclaringType().getName();
            String methodName = jp.getSignature().getName();
            LoggerFactory.getLogger(clazzName).debug("{}::{}({})", step.value(), methodName, argStr);
        }
        return jp.proceed();
    }
}
