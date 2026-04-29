package com.example.recommendation_service.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
  Flux<RecommendationEntity> findByProductId(int productId);
  Mono<RecommendationEntity> findByProductIdAndRecommendationId(int productId, int recommendationId) ;
}
