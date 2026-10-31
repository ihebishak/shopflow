package com.shopflow.catalog.application.api.command.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Le prix doit être strictement positif et en euros. Un prix absent est accepté : c'est le rôle de {@code @NotNull}.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EuroPriceValidator.class)
public @interface EuroPrice {

    String message() default "price must be a positive amount in euros";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
