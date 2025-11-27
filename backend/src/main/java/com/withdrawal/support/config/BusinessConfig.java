package com.withdrawal.support.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "business")
@Getter
@Setter
public class BusinessConfig {
    private int daysThreshold = 2;
}





