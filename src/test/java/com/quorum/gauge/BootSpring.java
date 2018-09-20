package com.quorum.gauge;

import com.thoughtworks.gauge.BeforeScenario;
import com.thoughtworks.gauge.ClassInitializer;
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
        @BeforeScenario
        public void beforeScenario() {
            System.out.println();
        }
    }
}
