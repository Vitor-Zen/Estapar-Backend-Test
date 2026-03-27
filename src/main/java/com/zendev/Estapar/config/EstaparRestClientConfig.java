package com.zendev.Estapar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EstaparRestClientConfig {

    @Bean
    public RestClient estaparRestClient(RestClient.Builder builder){
        return builder
                .baseUrl("http://localhost:3000")
                .build();
    }
}