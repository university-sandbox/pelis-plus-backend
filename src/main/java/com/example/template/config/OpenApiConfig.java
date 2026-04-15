package com.example.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${APP_PUBLIC_BASE_URL:http://localhost:${SERVER_PORT:8080}}")
    private String publicBaseUrl;

    @Bean
    public OpenAPI appOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Spring Boot JWT PostgreSQL Template API")
                .description("Boilerplate API with JWT auth and PostgreSQL")
                .version("v1")
                .contact(new Contact().name("Template Maintainer").email("maintainer@example.com")))
            .addServersItem(new Server().url(publicBaseUrl))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .schemaRequirement("bearerAuth", new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}
