package com.example.product_composite_service.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
import com.example.api.core.review.Review;
import com.example.api.core.review.ReviewService;
import com.example.api.event.Event;
import com.example.api.exceptions.InvalidInputException;
import com.example.api.exceptions.NotFoundException;
import com.example.util.HttpErrorInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.example.api.event.Event.Type.*;
import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Mono.empty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
  import org.springframework.boot.health.contributor.Health;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final WebClient webClient;
  private final ObjectMapper mapper;

  private static final String PRODUCT_SERVICE_URL = "http://product-service";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation-service";
  private static final String REVIEW_SERVICE_URL = "http://review-service";


  private final StreamBridge streamBridge;

  private final Scheduler publishEventScheduler;

  public ProductCompositeIntegration(
      @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
      StreamBridge streamBridge,
      WebClient.Builder webClientBuilder,
      ObjectMapper mapper
) {

    this.webClient = webClientBuilder.build();
    this.mapper = mapper;
    this.publishEventScheduler = publishEventScheduler;
    this.streamBridge = streamBridge;
  }

  // ── Product ────────────────────────────────────────────────────────────────

  // @Override
  // public Mono<Product> createProduct(Product body) {
  // return webClient.post()
  // .uri(productServiceUrl)
  // .bodyValue(body)
  // .retrieve()
  // .bodyToMono(Product.class)
  // .doOnSuccess(p -> LOG.debug("Created a product with id: {}",
  // p.getProductId()))
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  @Override
  public Mono<Product> createProduct(Product body) {

    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    String url = PRODUCT_SERVICE_URL + "/product/" + productId;
    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE)
        .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
  }

  // @Override
  // public Mono<Product> getProduct(int productId) {
  // return webClient.get()
  // .uri(productServiceUrl + "/" + productId)
  // .retrieve()
  // .bodyToMono(Product.class)
  // .doOnSuccess(p -> LOG.debug("Found a product with id: {}", p.getProductId()))
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  @Override
  public Mono<Void> deleteProduct(int productId) {

    return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event<Integer, Product>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  // @Override
  // public Mono<Void> deleteProduct(int productId) {
  // return webClient.delete()
  // .uri(productServiceUrl + "/" + productId)
  // .retrieve()
  // .bodyToMono(Void.class)
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  // ── Recommendation ─────────────────────────────────────────────────────────

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {

    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  // @Override
  // public Mono<Recommendation> createRecommendation(Recommendation body) {
  // return webClient.post()
  // .uri(recommendationServiceUrl)
  // .bodyValue(body)
  // .retrieve()
  // .bodyToMono(Recommendation.class)
  // .doOnSuccess(r -> LOG.debug("Created a recommendation for productId: {}",
  // r.getProductId()))
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {

    String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    // Return an empty result if something goes wrong to make it possible for the
    // composite service to return partial responses
    return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class).log(LOG.getName(), FINE)
        .onErrorResume(error -> empty());
  }

  // @Override
  // public Flux<Recommendation> getRecommendations(int productId) {
  // return webClient.get()
  // .uri(recommendationServiceUrl + "?productId=" + productId)
  // .retrieve()
  // .bodyToFlux(Recommendation.class)
  // .doOnComplete(() -> LOG.debug("Found recommendations for productId: {}",
  // productId))
  // .onErrorResume(ex -> {
  // // recommendations are non-critical — return empty rather than failing the
  // whole
  // // request
  // LOG.warn("Got an exception while requesting recommendations, returning empty:
  // {}", ex.getMessage());
  // return Flux.empty();
  // });
  // }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {

    return Mono
        .fromRunnable(
            () -> sendMessage("recommendations-out-0", new Event<Integer, Recommendation>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  // @Override
  // public Mono<Void> deleteRecommendations(int productId) {
  // return webClient.delete()
  // .uri(recommendationServiceUrl + "?productId=" + productId)
  // .retrieve()
  // .bodyToMono(Void.class)
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  // ── Review ─────────────────────────────────────────────────────────────────

  @Override
  public Mono<Review> createReview(Review body) {

    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  // @Override
  // public Mono<Review> createReview(Review body) {
  // return webClient.post()
  // .uri(reviewServiceUrl)
  // .bodyValue(body)
  // .retrieve()
  // .bodyToMono(Review.class)
  // .doOnSuccess(r -> LOG.debug("Created a review for productId: {}",
  // r.getProductId()))
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  @Override
  public Flux<Review> getReviews(int productId) {

    String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;

    LOG.debug("Will call the getReviews API on URL: {}", url);

    // Return an empty result if something goes wrong to make it possible for the
    // composite service to return partial responses
    return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE)
        .onErrorResume(error -> empty());
  }

  // @Override
  // public Flux<Review> getReviews(int productId) {
  // return webClient.get()
  // .uri(reviewServiceUrl + "?productId=" + productId)
  // .retrieve()
  // .bodyToFlux(Review.class)
  // .doOnComplete(() -> LOG.debug("Found reviews for productId: {}", productId))
  // .onErrorResume(ex -> {
  // LOG.warn("Got an exception while requesting reviews, returning empty: {}",
  // ex.getMessage());
  // return Flux.empty();
  // });
  // }

  @Override
  public Mono<Void> deleteReviews(int productId) {

    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<Integer, Review>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  // @Override
  // public Mono<Void> deleteReviews(int productId) {
  // return webClient.delete()
  // .uri(reviewServiceUrl + "?productId=" + productId)
  // .retrieve()
  // .bodyToMono(Void.class)
  // .onErrorMap(WebClientResponseException.class,
  // this::handleHttpClientException);
  // }

  // ── Error handling ─────────────────────────────────────────────────────────

  public Mono<Health> getProductHealth() {
    return getHealth(PRODUCT_SERVICE_URL);
  }

  public Mono<Health> getRecommendationHealth() {
    return getHealth(RECOMMENDATION_SERVICE_URL);
  }

  public Mono<Health> getReviewHealth() {
    return getHealth(REVIEW_SERVICE_URL);
  }

  private Mono<Health> getHealth(String url) {
    url += "/actuator/health";
    LOG.debug("Will call the Health API on URL: {}", url);
    return webClient.get().uri(url).retrieve().bodyToMono(String.class)
        .map(s -> new Health.Builder().up().build())
        .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
        .log(LOG.getName(), FINE);
  }

  private <T> void sendMessage(String bindingName, Event<Integer, T> event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
    Message<Event<Integer, T>> message = MessageBuilder.withPayload(event)
        .setHeader("partitionKey", event.getKey())
        .build();
    streamBridge.send(bindingName, message);
  }

  private Throwable handleException(Throwable ex) {

    if (!(ex instanceof WebClientResponseException)) {
      LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
      return ex;
    }

    WebClientResponseException wcre = (WebClientResponseException) ex;

    switch (HttpStatus.resolve(wcre.getStatusCode().value())) {

      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(wcre));

      case UNPROCESSABLE_CONTENT:
        return new InvalidInputException(getErrorMessage(wcre));

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JacksonException ioex) {
      return ex.getMessage();
    }
  }
}