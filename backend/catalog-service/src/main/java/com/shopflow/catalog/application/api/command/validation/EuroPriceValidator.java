package com.shopflow.catalog.application.api.command.validation;

import com.shopflow.kernel.money.Money;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Vérifie {@link EuroPrice}.
 */
public class EuroPriceValidator implements ConstraintValidator<EuroPrice, Money> {

    private static final String EURO = "EUR";

    @Override
    public boolean isValid(final Money price, final ConstraintValidatorContext context) {
        return price == null || (price.getAmount() > 0 && EURO.equals(price.getCurrency().getCurrencyCode()));
    }
}
