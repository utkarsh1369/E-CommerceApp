package com.microservices.user_service.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(tags = {
        @Tag(name = "Authorization APIs"),
        @Tag(name = "User APIs")
})
public class SwaggerConfig {

        @Bean
        public OpenAPI myCustomConfig() {
            return new OpenAPI().info(
                    new Info().title("User Service APIs").description("By Utkarsh")
            ).components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .in(SecurityScheme.In.HEADER)
                                    .name("Authorization"))
            ).servers(
                    List.of(
                            new Server().url("http://localhost:8081").description("Local server"),
                            new Server().url("http://localhost:8085").description("Gateway server")
                    ));
        }
}
