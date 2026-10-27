// filepath: /backend/src/main/java/com/example/app/shared/config/ClockConfig.java
package com.example.app.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Clock sebagai bean agar logika berbasis waktu mudah dites. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
