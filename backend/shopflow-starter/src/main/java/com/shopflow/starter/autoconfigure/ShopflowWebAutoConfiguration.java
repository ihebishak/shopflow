package com.shopflow.starter.autoconfigure;

import com.shopflow.starter.correlation.CorrelationIdFilter;
import com.shopflow.starter.web.ProblemDetailsExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Services web (servlet) : corrélation des requêtes HTTP et erreurs au format Problem Details.
 * Aussi chargée par les tests {@code @WebMvcTest} (voir {@code AutoConfigureWebMvc.imports}).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ShopflowWebAutoConfiguration {

    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ResponseEntityExceptionHandler.class)
    static class ProblemDetails {

        @Bean
        @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
        ProblemDetailsExceptionHandler problemDetailsExceptionHandler() {
            return new ProblemDetailsExceptionHandler();
        }
    }
}
