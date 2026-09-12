package com.example.hms.config;

import jakarta.validation.ValidatorFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class ValidationConfiguration {
    @Bean
    ValidationConfigurationCustomizer hospitalValidationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }

    @Bean
    HibernatePropertiesCustomizer persistenceValidation(ValidatorFactory validatorFactory) {
        return properties -> properties.put("jakarta.persistence.validation.factory", validatorFactory);
    }
}
