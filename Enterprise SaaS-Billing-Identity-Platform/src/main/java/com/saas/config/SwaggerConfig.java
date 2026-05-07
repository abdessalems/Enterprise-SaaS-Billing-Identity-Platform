package com.saas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";


    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Enterprise SaaS Platform API")
                .description("Production-ready SaaS backend — billing, identity, and multi-tenancy.\n\n" +
                    "**How to authenticate:**\n" +
                    "1. Call `POST /auth/register` or `POST /auth/login`\n" +
                    "2. Copy the `accessToken` from the response\n" +
                    "3. Click **Authorize** above and paste: `<your-token>`")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Abdessalem Saadaoui")
                    .email("abdessalemsaa@gmail.com"))
                .license(new License()
                    .name("MIT License")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Paste your JWT access token here")));
    }
}
