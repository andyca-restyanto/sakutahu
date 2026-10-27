// filepath: /backend/src/main/java/com/example/app/shared/config/AsyncConfig.java
package com.example.app.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    public static final String MAIL_EXECUTOR = "mailExecutor";
    public static final String RECEIPT_EXECUTOR = "receiptExecutor";

    /** Pembacaan struk (unduh foto + panggil AI vision), beberapa detik per struk. */
    @Bean(name = RECEIPT_EXECUTOR)
    public Executor receiptExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("receipt-");
        executor.initialize();
        return executor;
    }

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }
}
