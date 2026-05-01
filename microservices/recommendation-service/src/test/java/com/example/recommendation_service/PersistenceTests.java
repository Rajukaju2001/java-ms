package com.example.recommendation_service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import com.example.recommendation_service.persistence.RecommendationEntity;
import com.example.recommendation_service.persistence.RecommendationRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Import(TestcontainersConfiguration.class)
@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})
class PersistenceTests {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity;

  @BeforeEach
  void setupDb() {

    StepVerifier
        .create(repository.deleteAll())
        .verifyComplete();

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");

    StepVerifier
        .create(repository.save(entity))
        .assertNext(saved -> {
           savedEntity = saved; 
          assertEqualsRecommendation(savedEntity, entity) ;
        })
        .verifyComplete();
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");

    StepVerifier
        .create(repository.save(newEntity))
        .assertNext(saved -> assertEqualsRecommendation(newEntity, saved))
        .verifyComplete();

    StepVerifier
        .create(repository.count())
        .expectNext(2L)
        .verifyComplete();

  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    StepVerifier
        .create(repository.save(savedEntity).flatMap(saved -> repository.findById(saved.getId())))
        .assertNext(found -> {
          assertEquals(1, (long) found.getVersion());
          assertEquals("a2", found.getAuthor());
        })
        .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier
        .create(repository.delete(savedEntity))
        .verifyComplete();

    StepVerifier
        .create(repository.existsById(savedEntity.getId()))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void getByProductId() {
    StepVerifier
        .create(repository.findByProductId(savedEntity.getProductId()))
        .assertNext(found -> assertEqualsRecommendation(savedEntity, found))
        .verifyComplete(); // also asserts only 1 item was emitted
  }

  @Test
  void optimisticLockError() {
    // Fetch the same record twice — two separate objects, same version
    StepVerifier
        .create(
            repository.findById(savedEntity.getId())
                .zipWith(repository.findById(savedEntity.getId()))
                .flatMap(tuple -> {
                  RecommendationEntity entity1 = tuple.getT1();
                  RecommendationEntity entity2 = tuple.getT2();

                  entity1.setAuthor("a1");

                  // Save entity1 first — bumps version to 1
                  return repository.save(entity1)
                      .then(Mono.fromCallable(() -> {
                        entity2.setAuthor("a2");
                        return entity2;
                      }))
                      // Now save entity2 — still on version 0, should fail
                      .flatMap(repository::save);
                }))
        .expectError(OptimisticLockingFailureException.class)
        .verify();

    // Verify final state: only entity1's update persisted
    StepVerifier
        .create(repository.findById(savedEntity.getId()))
        .assertNext(updated -> {
          assertEquals(1, (int) updated.getVersion());
          assertEquals("a1", updated.getAuthor());
        })
        .verifyComplete();
  }

  private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    assertEquals(expectedEntity.getId(), actualEntity.getId());
    assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
    assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
    assertEquals(expectedEntity.getAuthor(), actualEntity.getAuthor());
    assertEquals(expectedEntity.getRating(), actualEntity.getRating());
    assertEquals(expectedEntity.getContent(), actualEntity.getContent());
  }
}