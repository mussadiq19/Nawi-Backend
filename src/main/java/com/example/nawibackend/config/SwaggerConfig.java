package com.example.nawibackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI nawiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nawi Backend API")
                        .description("API documentation for Nawi Backend")
                        .version("v1"));
    }
}
