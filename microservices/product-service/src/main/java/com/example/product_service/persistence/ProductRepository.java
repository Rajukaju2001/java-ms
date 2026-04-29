package com.example.product_service.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveSortingRepository<ProductEntity, String>, ReactiveCrudRepository<ProductEntity, String> {
  Flux<ProductEntity> findAllByOrderByProductIdAsc(Pageable pageable);
  Mono<ProductEntity> findByProductId(int productId);
}