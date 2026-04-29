package com.example.util;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import com.example.api.exceptions.InvalidInputException;
import com.example.api.exceptions.NotFoundException;

import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(-2) // must run before Spring's default error handler
public class ReactiveExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public ReactiveExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof InvalidInputException) {
            status = HttpStatus.UNPROCESSABLE_CONTENT;
        } else {
            return Mono.error(ex); // not ours — let Spring handle it
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpErrorInfo errorInfo = new HttpErrorInfo(
            status,
            exchange.getRequest().getPath().pathWithinApplication().value(),
            ex.getMessage()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorInfo);
        } catch (JacksonException e) {
            return Mono.error(e);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
