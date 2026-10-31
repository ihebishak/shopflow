package com.shopflow.catalog.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Informations générales de la documentation OpenAPI (en-tête de la page Swagger UI).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI catalogOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ShopFlow — Catalogue")
                .version("v1")
                .description("Produits des vendeurs de la marketplace sneakers et streetwear : création, "
                        + "consultation, puis variantes, prix et publication."));
    }
}
