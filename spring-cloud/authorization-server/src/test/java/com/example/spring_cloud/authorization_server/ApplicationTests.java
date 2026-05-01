package com.example.spring_cloud.authorization_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
