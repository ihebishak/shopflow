package com.shopflow.kernel.error;

import lombok.Getter;

import java.util.Objects;

/**
 * Base de toutes les erreurs métier de ShopFlow, transformées en réponses Problem Details par le starter.
 * <p>
 * Chaque service définit ses propres sous-classes (ex. {@code ProductNotFoundException}), avec un code stable.
 * Le message est en anglais ; le frontend affiche le texte français à partir du code.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorKind kind;

    /** Code stable en UPPER_SNAKE_CASE (ex. {@code PRODUCT_NOT_FOUND}), traduit par le frontend. */
    private final String code;

    /** Champ en cause, {@code null} si l'erreur ne porte pas sur un champ précis. */
    private final String field;

    protected BusinessException(final ErrorKind kind, final String code, final String message) {
        this(kind, code, null, message);
    }

    protected BusinessException(final ErrorKind kind, final String code, final String field, final String message) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = Objects.requireNonNull(code, "code");
        this.field = field;
    }
}
