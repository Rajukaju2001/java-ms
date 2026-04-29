package com.example.recommendation_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
import com.example.api.exceptions.InvalidInputException;
import com.example.recommendation_service.persistence.RecommendationEntity;
import com.example.recommendation_service.persistence.RecommendationRepository;
import com.example.util.ServiceUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final RecommendationRepository repository;

  private final RecommendationMapper mapper;

  private final ServiceUtil serviceUtil;

  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,
      ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    RecommendationEntity entity = mapper.apiToEntity(body);

    return repository
        .findByProductIdAndRecommendationId(entity.getProductId(), entity.getRecommendationId())
        // If a record IS found, flatMap fires — emit an error
        .flatMap(existing -> Mono.<RecommendationEntity>error(
            new DuplicateKeyException(
                "Duplicate key, Product Id: " + body.getProductId()
                    + ", Recommendation Id: " + body.getRecommendationId())))
        // If no record found (empty), fall through to save
        .switchIfEmpty(repository.save(entity))
        .map(mapper::entityToApi)
        .doOnSuccess(r -> LOG.debug(
            "createRecommendation: created recommendation {}/{}",
            body.getProductId(), body.getRecommendationId()))
        .onErrorMap(DuplicateKeyException.class, dke -> new InvalidInputException(dke.getMessage()));
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    if (productId < 1) {
      // Flux.error is the reactive equivalent of throwing in a non-reactive method
      return Flux.error(new InvalidInputException("Invalid productId: " + productId));
    }

    return repository.findByProductId(productId)
        .map(entity -> {
          Recommendation rec = mapper.entityToApi(entity);
          rec.setServiceAddress(serviceUtil.getServiceAddress());
          return rec;
        })
        .doOnComplete(() -> LOG.debug("getRecommendations: finished streaming for productId: {}", productId));
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    LOG.debug("deleteRecommendations: tries to delete recommendations for productId: {}", productId);

    return repository.findByProductId(productId)
        .collectList()
        .flatMap(repository::deleteAll);
  }
}