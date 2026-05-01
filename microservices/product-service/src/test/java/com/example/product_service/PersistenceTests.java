package com.example.product_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.product_service.persistence.ProductEntity;
import com.example.product_service.persistence.ProductRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Import(TestcontainersConfiguration.class)
@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})
public class PersistenceTests {

    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    void setupDb() {
        StepVerifier.create(
                repository.deleteAll())
                .verifyComplete();

        ProductEntity entity = new ProductEntity(1, "n", 1);

        StepVerifier.create(repository.save(entity))
                .assertNext(saved -> {
                    savedEntity = saved;
                    assertEqualsProduct(entity, savedEntity);

                })
                .verifyComplete();

    }

    @Test
    void create() {

        ProductEntity newEntity = new ProductEntity(2, "n", 2);

        StepVerifier.create(
                repository.save(newEntity)
                        .flatMap(saved -> repository.findById(saved.getId()))
                        .zipWith(repository.count()))
                .assertNext(tuple -> {
                    assertEqualsProduct(newEntity, tuple.getT1());
                    assertEquals(2L, tuple.getT2());
                })
                .verifyComplete();

    }

    @Test
    void update() {
        savedEntity.setName("n2");

        StepVerifier.create(
                repository.save(savedEntity)
                        .flatMap(saved -> repository.findById(saved.getId())))
                .assertNext(foundEntity -> {
                    assertEquals(1, (long) foundEntity.getVersion());
                    assertEquals("n2", foundEntity.getName());
                })
                .verifyComplete();
    }

    @Test
    void delete() {
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(savedEntity.getId()))
                .assertNext(flag -> assertFalse(flag))
                .verifyComplete();
    }

    @Test
    void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
                .assertNext(entity -> {
                    assertEqualsProduct(savedEntity, entity);
                })
                .verifyComplete();
    }

    @Test
    void optimisticLockError() {

        // ✅ split into two StepVerifiers like the recommendation test
        StepVerifier.create(
                Mono.zip(
                        repository.findById(savedEntity.getId()),
                        repository.findById(savedEntity.getId()))
                        .flatMap(tuple -> {
                            ProductEntity entity1 = tuple.getT1();
                            ProductEntity entity2 = tuple.getT2();
                            entity1.setName("n1");
                            return repository.save(entity1)
                                    .then(Mono.defer(() -> {
                                        entity2.setName("n2");
                                        return repository.save(entity2); // should fail
                                    }));
                        }))
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        // then verify final state separately
        StepVerifier.create(repository.findById(savedEntity.getId()))
                .assertNext(updated -> {
                    assertEquals(1, (int) updated.getVersion());
                    assertEquals("n1", updated.getName());
                })
                .verifyComplete();
    }

    @Test
    void paging() {
        // Clear out old data
        StepVerifier.create(repository.deleteAll()).verifyComplete();

        // Insert 10 new products
        List<ProductEntity> newProducts = IntStream.rangeClosed(1001, 1010)
                .mapToObj(i -> new ProductEntity(i, "name " + i, i))
                .collect(Collectors.toList());

        StepVerifier.create(repository.saveAll(newProducts).then()).verifyComplete();

        // Page through results
        Pageable nextPage = PageRequest.of(0, 4, Sort.Direction.ASC, "productId");
        nextPage = testNextPage(nextPage, Arrays.asList(1001, 1002, 1003, 1004), true);
        nextPage = testNextPage(nextPage, Arrays.asList(1005, 1006, 1007, 1008), true);
        testNextPage(nextPage, Arrays.asList(1009, 1010), false);
    }

    private Pageable testNextPage(Pageable pageable, List<Integer> expectedIds, boolean expectsNextPage) {
        StepVerifier.create(repository.findAllByOrderByProductIdAsc(pageable).collectList())
                .assertNext(list -> {
                    List<Integer> ids = list.stream().map(ProductEntity::getProductId).collect(Collectors.toList());
                    assertEquals(expectedIds, ids);
                    assertEquals(expectsNextPage, list.size() == pageable.getPageSize());
                })
                .verifyComplete();

        return pageable.next();
    }

    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
        assertEquals(expectedEntity.getName(), actualEntity.getName());
        assertEquals(expectedEntity.getWeight(), actualEntity.getWeight());
    }
}