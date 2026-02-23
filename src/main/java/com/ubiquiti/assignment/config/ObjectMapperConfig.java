package com.ubiquiti.assignment.config;

import io.vavr.jackson.datatype.*;
import org.springframework.context.annotation.*;

@Configuration
public class ObjectMapperConfig {

    @Bean
    VavrModule vavrModule() {
        return new VavrModule();
    }
}
