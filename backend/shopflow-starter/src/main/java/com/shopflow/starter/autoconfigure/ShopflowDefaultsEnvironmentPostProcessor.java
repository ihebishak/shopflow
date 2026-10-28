package com.shopflow.starter.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Valeurs par défaut communes à tous les services, ajoutées avec la priorité la plus basse :
 * un service peut toujours les redéfinir dans son application.yml.
 */
public class ShopflowDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "shopflowDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(
                // Chaque ligne de log affiche l'identifiant de corrélation du parcours en cours
                "logging.pattern.level", "%5p [%X{correlationId:-}]")));
    }
}
