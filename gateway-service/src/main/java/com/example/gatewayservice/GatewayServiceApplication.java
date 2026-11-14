package com.example.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

    private final EurekaDiscoveryClient discoveryClient;

    public GatewayServiceApplication(EurekaDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

@Bean
    DiscoveryClientRouteDefinitionLocator rotes(ReactiveDiscoveryClient rdc, DiscoveryLocatorProperties dp ){
        return new DiscoveryClientRouteDefinitionLocator(rdc,dp);
    }

}
