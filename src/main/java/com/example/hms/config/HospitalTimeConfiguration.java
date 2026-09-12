package com.example.hms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class HospitalTimeConfiguration {
    @Bean
    public Clock hospitalClock(@Value("${hospital.time-zone:Asia/Dhaka}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
