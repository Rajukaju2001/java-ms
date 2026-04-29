package com.example.review_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication(scanBasePackages = "com.example")
public class ReviewServiceApplication {

	@Value("${app.threadPoolSize:10}")
	Integer threadPoolSize;

	@Value("${app.taskQueueSize:100}")
	Integer taskQueueSize ;

	public static void main(String[] args) {
		SpringApplication.run(ReviewServiceApplication.class, args);
	}

	@Bean(name = "jdbcScheduler")
	Scheduler jdbcScheduler() {
		return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");

	}

}
