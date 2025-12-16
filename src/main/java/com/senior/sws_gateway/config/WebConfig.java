package com.senior.sws_gateway.config;

import com.senior.sws_gateway.interceptor.ApiKeyValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Web configuration for registering interceptors
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyValidationInterceptor apiKeyValidationInterceptor;

    public WebConfig(ApiKeyValidationInterceptor apiKeyValidationInterceptor) {
        this.apiKeyValidationInterceptor = apiKeyValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register API key validation interceptor for all paths
        // This will validate API keys for all incoming requests
        registry.addInterceptor(apiKeyValidationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/error"); // Exclude health check and error endpoints
    }
    
    /**
     * Configure RestTemplate for proxy client with appropriate timeouts
     * Requirements: Configure timeout and error handling for proxy requests
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}