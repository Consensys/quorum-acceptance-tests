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

import com.quorum.gauge.Main;
import com.thoughtworks.gauge.BeforeScenario;
import com.thoughtworks.gauge.ClassInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

public class BootSpring implements ClassInitializer {

    private ApplicationContext context;

    public BootSpring() {
        // workaround for JDK11: https://bugs.openjdk.java.net/browse/JDK-8213202
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        context = SpringApplication.run(Main.class);
    }

    @Override
    public Object initialize(Class<?> classToInitialize) throws Exception {
        try {
            return context.getBean(classToInitialize);
        } catch (RuntimeException re) {
            throw new Exception(re);
        }
    }

    @Service
    public static class FixNewLine {
        private static final Logger logger = LoggerFactory.getLogger(FixNewLine.class);

        @BeforeScenario
        public void beforeScenario() {
            if (logger.isDebugEnabled()) {
                System.out.println();
            }
        }
    }
}
