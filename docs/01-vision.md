# 01 — Vision, personas et parcours

## Vision

> **ShopFlow est la marketplace sneakers & streetwear où des vendeurs indépendants lancent leurs drops, et où chaque achat se vit en temps réel.**

Le temps réel n'est pas un gadget technique, c'est le cœur de l'expérience : on voit le stock fondre pendant un drop, la commande avancer étape par étape, le colis se déplacer, les tendances bouger.

## Univers produit : sneakers & streetwear

- **Catégories :** sneakers, hoodies & sweats, t-shirts, pantalons, casquettes & accessoires.
- **Variantes :** chaque produit se décline en tailles (EU 36 → 47 pour les sneakers, XS → XXL pour les vêtements) et en coloris. **Le stock est géré par variante (SKU)**.
- **Culture des drops :** sorties programmées, éditions limitées, ventes flash : c'est ce qui justifie l'architecture temps réel.

## Personas

| Persona | Profil | Objectifs | Frustrations ailleurs |
|---|---|---|---|
| 🛍️ **Sarah, client·e, 27 ans** | Sneakerhead, achète surtout sur mobile | Ne rater aucun drop, payer vite, suivre son colis | Sites qui plantent pendant les drops, « en stock » mensongers, faux avis |
| 🏪 **Karim, vendeur** | Petite boutique streetwear indépendante | Publier facilement, gérer le stock par taille, voir ses ventes et ses revenus en direct | Outils lourds, aucune visibilité sur ses chiffres et ses reversements |
| 🛡️ **Admin ShopFlow** | Équipe qui opère la plateforme | Valider les vendeurs, programmer les drops, modérer, suivre les commissions | Faux vendeurs, fraude, manque de visibilité |

## Parcours principaux

### Client
1. **Achat classique :** rechercher (filtres taille, marque, prix) → fiche produit → choisir sa taille → panier → **paiement Stripe** → suivi de commande en direct → **suivi du colis** → avis.
2. **Drop / vente flash :** notification avant l'ouverture → file d'attente virtuelle → achat en temps limité → confirmation immédiate ou refus clair (jamais de survente).
3. **Achat malin :** wishlist → alerte baisse de prix ou retour en stock **dans sa taille** → achat.

### Vendeur
4. **Onboarding :** demande de compte vendeur → validation admin → création de la boutique.
5. **Vente :** publier un produit et ses variantes → recevoir une sous-commande → préparer → **remettre au transporteur** → suivre la livraison.
6. **Revenus :** voir son chiffre d'affaires, la commission prélevée et le solde à reverser ; recevoir ses reversements périodiques.

### Admin
7. **Supervision :** valider les vendeurs, programmer un drop, modérer les avis, suivre l'activité et les commissions de la plateforme.

## Décisions métier

| Sujet | Décision | Conséquence technique |
|---|---|---|
| Univers | Sneakers & streetwear | Stock par variante (taille × coloris) |
| Modèle économique | **Commission de 10 %** prélevée sur chaque vente (paramétrable) | Nouveau service `payout-service` : registre des montants dus par vendeur, reversements périodiques simulés |
| Paiement | **Stripe en mode test** (Payment Element intégré + webhooks) | Paiement **asynchrone** : la commande attend la confirmation de Stripe ; une réservation non payée expire (30 min en classique, 10 min pour un drop) |
| Livraison | **Transporteur simulé** | Nouveau service `shipping-service` : n° de suivi, statuts qui avancent automatiquement (pris en charge → en transit → en livraison → livré), incidents occasionnels (retard) |
| Commission prélevée quand ? | Une fois la sous-commande **livrée** | Le reversement vendeur dépend de l'événement de livraison |
| Langue / devise | Français, euro | — |

## Hors périmètre (pour l'instant)

- Vrais paiements et vrais virements aux vendeurs (Stripe reste en mode test).
- Seconde main / revente entre particuliers.
- Application mobile native (le frontend sera responsive).

## Idées en attente (à discuter plus tard)

- **Drops par tirage au sort (raffle)** : alternative au « premier arrivé, premier servi », très présente dans la culture sneakers.
- **Stripe Connect** pour un vrai partage des paiements entre plateforme et vendeurs (en mode test).
- **Annulation d'une sous-commande par le vendeur** (rupture réelle, article abîmé) : remboursement partiel et adaptation du processus de commande.
