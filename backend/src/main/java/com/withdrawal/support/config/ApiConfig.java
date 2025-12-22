package com.withdrawal.support.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
@Getter
@Setter
public class ApiConfig {

    private DataEntryConfig dataentry;
    private FormServiceConfig formservice;
    private CaseDetailsConfig casedetails;
    private OnBaseConfig onbase;

    @Getter
    @Setter
    public static class DataEntryConfig {
        private String url;
        private String key;
    }

    @Getter
    @Setter
    public static class FormServiceConfig {
        private String url;
        private String key;
    }

    @Getter
    @Setter
    public static class CaseDetailsConfig {
        private String url;
        private String key;
    }

    @Getter
    @Setter
    public static class OnBaseConfig {
        private String url;
        private String authorization;  // Basic Auth header value
    }
}

