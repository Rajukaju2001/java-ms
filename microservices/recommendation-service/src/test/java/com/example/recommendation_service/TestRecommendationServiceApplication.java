package com.example.recommendation_service;

import org.springframework.boot.SpringApplication;

public class TestRecommendationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.from(RecommendationServiceApplication::main)
            .with(TestcontainersConfiguration.class).run(args);
    }
}
