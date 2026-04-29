package com.example.product_composite_service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map ;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Map<String, ServiceConfig> services ;
    
    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    };   
    
    public static class ServiceConfig {
        private String host;
        private int port;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

}
