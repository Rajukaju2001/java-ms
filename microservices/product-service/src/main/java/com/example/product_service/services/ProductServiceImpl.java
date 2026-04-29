package com.example.product_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.exceptions.InvalidInputException;
import com.example.api.exceptions.NotFoundException;
import com.example.product_service.persistence.ProductEntity;
import com.example.product_service.persistence.ProductRepository;
import com.example.util.ServiceUtil;

import reactor.core.publisher.Mono;

@RestController
public class ProductServiceImpl implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

  private final ServiceUtil serviceUtil;

  private final ProductRepository repository;

  private final ProductMapper mapper;

  public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {

    ProductEntity entity = mapper.apiToEntity(body);

    return repository
        .findByProductId(entity.getProductId())
        .flatMap(existing -> Mono.<ProductEntity>error(new DuplicateKeyException("Duplicate key, Product Id: " + existing.getProductId())))
        .switchIfEmpty(repository.save(entity))
        .map(mapper::entityToApi)
        .doOnSuccess(r -> LOG.debug("createProduct: entity created for productId: {}", r.getProductId()))
        .onErrorMap(DuplicateKeyException.class, dke -> new InvalidInputException(dke.getMessage()));
  }

  @Override
  public Mono<Product> getProduct(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    
    return repository
      .findByProductId(productId)
      .map(mapper::entityToApi)
      .map(r -> {
        r.setServiceAddress(serviceUtil.getServiceAddress()) ;
        return r;
      })
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId))) 
      .doOnSuccess(r -> LOG.debug("getProduct: found productId: {}", r.getProductId()) );
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    return repository
      .findByProductId(productId)
      .flatMap(found -> repository.delete(found)) ;
  }
}