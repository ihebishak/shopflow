package com.shopflow.kernel.money;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Currency;
import java.util.Objects;

/**
 * Montant d'argent exprimé en unités mineures (centimes pour l'euro). Immuable et comparé par valeur.
 * <p>
 * Jamais de {@code double} pour de l'argent : tous les calculs se font sur des entiers.
 */
@Getter
@EqualsAndHashCode
public final class Money implements Comparable<Money> {

    private static final long BASIS_POINTS_PER_UNIT = 10_000;

    /** Montant en unités mineures (ex. 16000 = 160,00 €). */
    private final long amount;

    private final Currency currency;

    public Money(long amount, Currency currency) {
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money euros(long cents) {
        return of(cents, "EUR");
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(amount, other.amount), currency);
    }

    public Money times(long quantity) {
        return new Money(Math.multiplyExact(amount, quantity), currency);
    }

    /**
     * Applique un pourcentage exprimé en points de base (1 000 = 10 %), arrondi à l'unité mineure inférieure.
     * Exemple : 10 % de 159,99 € = 15,99 €.
     */
    public Money percentageFloor(long basisPoints) {
        if (basisPoints < 0) {
            throw new IllegalArgumentException("basisPoints must be positive: " + basisPoints);
        }
        return new Money(Math.floorDiv(Math.multiplyExact(amount, basisPoints), BASIS_POINTS_PER_UNIT), currency);
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(amount, other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        long unit = (long) Math.pow(10, currency.getDefaultFractionDigits());
        String sign = amount < 0 ? "-" : "";
        long absolute = Math.abs(amount);
        if (unit == 1) {
            return sign + absolute + " " + currency.getCurrencyCode();
        }
        return String.format("%s%d.%0" + currency.getDefaultFractionDigits() + "d %s",
                sign, absolute / unit, absolute % unit, currency.getCurrencyCode());
    }
}
