package com.example.rqchallenge.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/**")
                .build();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Enable Swagger UI resources to be served
        registry.addResourceHandler("/webjars/**", "/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/",
                        "classpath:/META-INF/resources/swagger-ui/");
    }

}
