package com.shopflow.kernel.error;

/**
 * Nature d'une erreur métier. Le domaine ne parle jamais de HTTP : c'est le starter qui traduit en statut.
 */
public enum ErrorKind {

    /** Donnée invalide (champ manquant, trop long…) : 400. */
    INVALID,

    /** Ressource introuvable (ou invisible pour l'appelant) : 404. */
    NOT_FOUND,

    /** Action contraire à l'état actuel (ex. publier un produit déjà publié) : 409. */
    CONFLICT
}
