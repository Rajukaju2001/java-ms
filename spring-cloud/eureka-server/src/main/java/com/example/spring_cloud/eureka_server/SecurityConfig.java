package com.example.spring_cloud.eureka_server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private final String username;
  private final String password;

  public SecurityConfig(
    @Value("${app.eureka-username}") String username,
    @Value("${app.eureka-password}") String password
  ) {
    this.username = username;
    this.password = password;
  }

  @Bean
  InMemoryUserDetailsManager userDetailsService() {
    UserDetails user = User.builder()
        .username(username)
        .password("{noop}"+password)
        .roles("USER")
        .build();
    return new InMemoryUserDetailsManager(user);
  }

  @Bean
  SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
      // Disable CRCF to allow services to register themselves with Eureka
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(
        authz -> authz.anyRequest().authenticated()
      ).httpBasic(Customizer.withDefaults()) ;

    return http.build();
  }
}