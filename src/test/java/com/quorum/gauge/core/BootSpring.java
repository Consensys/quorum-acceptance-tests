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
