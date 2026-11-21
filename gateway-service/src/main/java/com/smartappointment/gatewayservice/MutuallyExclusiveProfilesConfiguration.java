package com.smartappointment.gatewayservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("statique & dynamique")
public class MutuallyExclusiveProfilesConfiguration {

    @Bean
    Object rejectConflictingGatewayProfiles() {
        throw new IllegalStateException("Activate only one gateway profile: statique or dynamique.");
    }
}
