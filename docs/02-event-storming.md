  # 02 — Event storming

Les événements sont nommés **au passé** (un fait qui s'est produit), en PascalCase. Les conventions techniques (topics, enveloppe, versioning) sont définies dans `03-conventions.md`.

## Principes transverses

- **Architecture hybride : orchestration + chorégraphie.**
  - Les **processus métier longs** (commande, onboarding vendeur, validation des drops) sont **orchestrés par Camunda 8** (BPMN).
  - Les **effets de bord** (notifications, analytics, fidélité, reversements) restent en **chorégraphie** : de simples consommateurs Kafka.
- **Commandes ≠ événements.**
  - Une **commande** demande une action à un service précis, à l'impératif : `ReserveStock` = « réserve le stock ». Un seul destinataire.
  - Un **événement** constate un fait, au passé : `StockReserved` = « le stock a été réservé ». Autant de consommateurs que nécessaire.
- **Seuls les services porteurs d'un processus dépendent de Camunda** (`order`, `user`, `promotion`). Leurs workers traduisent les tâches BPMN en commandes Kafka, et leurs consommateurs Kafka font avancer les processus (corrélation de messages). `inventory`, `payment`, `shipping`… ignorent l'existence de Camunda.
- **`order-service` (via son processus) est la seule source de vérité sur le sort d'une commande.**
- **Pas d'appel synchrone dans le chemin critique de la commande.** `order-service` tient sa propre copie des prix, alimentée par les événements du catalogue.
- **Event Sourcing ciblé : `payout-service` uniquement (Axon Framework).** Son grand livre est stocké sous forme d'**événements de domaine** privés (event store Axon) ; les soldes sont des projections (CQRS). Il publie sur Kafka des **événements d'intégration** stables (`SellerEarningRecorded`, `PayoutCompleted`…). Les autres services ignorent l'existence d'Axon. Choix motivé par l'apprentissage, à documenter dans un ADR.
- **Pas de données sensibles dans les événements** (IBAN, données de paiement…) : un événement est lu par de nombreux consommateurs et conservé dans Kafka. Les données sensibles passent uniquement par des commandes adressées à un seul service.
- **Les étapes importantes ont leur propre événement** (`ShipmentDelivered`, `OrderCompleted`), même s'il existe un événement générique de changement de statut.

---

## Parcours 1 — Achat classique

### Processus BPMN `order-fulfillment` (Camunda, workers dans `order-service`)

```
 (●) Commande validée par le client ══ OrderPlaced
  │
 [Réserver le stock] ──── cmd ReserveStock ────▶ inventory
  │
 <◇ attente> ── StockReserved ─────────────────────────────────────────┐
  ├── StockReservationFailed ──▶ [Annuler la commande] ══ OrderCancelled ──▶ (◉ Annulée)
  └── ⏱ 1 min sans réponse ───▶ incident technique (visible dans Operate)
                                                                         ▼
 [Créer le paiement] ──── cmd CreatePaymentIntent ────▶ payment ══ PaymentIntentCreated ─▶ front (formulaire Stripe)
  │
 <◇ attente> ── PaymentSucceeded ──────────────────────────────────────┐
  ├── PaymentFailed ───────────┐                                        │
  ├── ⏱ 30 min (10 min drop) ──┼─▶ [Libérer le stock] ─▶ [Annuler le paiement]
  └── CancellationRequested ───┘        cmd ReleaseStock     cmd CancelPaymentIntent
                                         ─▶ [Annuler la commande] ══ OrderCancelled ──▶ (◉ Annulée)
                                                                         ▼
 [Confirmer le stock] ── cmd ConfirmStockReservation ─▶ inventory
 [Confirmer la commande] ══ OrderConfirmed
  │
 ╔═ Pour chaque sous-commande (en parallèle, une par vendeur) ═══════════╗
 ║  <attente ShipmentCreated>   ⏱ 72 h ─▶ relance ══ SubOrderShipmentOverdue
 ║  <attente ShipmentDelivered>                                           ║
 ╚════════════════════════════════════════════════════════════════════════╝
  │   ◀── CancellationRequested (seulement si aucun colis n'est parti)
  │        └─▶ [Rembourser] cmd RefundPayment ─▶ <attente RefundIssued>
  │            ─▶ [Libérer le stock] ─▶ [Annuler la commande] ══ OrderCancelled ──▶ (◉ Annulée)
  ▼
 [Clôturer la commande] ══ OrderCompleted ──▶ (◉ Terminée)
```

Légende : `[tâche]` exécutée par un worker · `<◇ attente>` passerelle sur événements · `⏱` timer · `══` événement publié · `cmd` commande Kafka.

### Commandes (Kafka) — envoyées par le processus

| Commande | Destinataire | Réponse attendue par le processus |
|---|---|---|
| `ReserveStock` | inventory | `StockReserved` ou `StockReservationFailed` |
| `CreatePaymentIntent` | payment | `PaymentIntentCreated` |
| `ConfirmStockReservation` | inventory | — |
| `ReleaseStock` | inventory | — (inventory publie `StockReleased` pour information) |
| `CancelPaymentIntent` | payment | — |
| `RefundPayment` | payment | `RefundIssued` |

### Événements

| Événement | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `ProductPriceChanged` | catalog | order (copie locale des prix) | SKU, nouveau prix |
| `OrderPlaced` | order | notification, analytics | commande, client, sous-commandes par vendeur (SKU, quantité, prix), adresse, total |
| `StockReserved` | inventory | order (processus) | commande, expiration de la réservation |
| `StockReservationFailed` | inventory | order (processus) | commande, SKU indisponibles |
| `StockReleased` | inventory | (alertes retour en stock — parcours 3) | SKU, quantité libérée |
| `PaymentIntentCreated` | payment | order (processus + transmis au front) | commande, `client_secret` Stripe |
| `PaymentSucceeded` | payment | order (processus) | commande, id paiement Stripe, montant |
| `PaymentFailed` | payment | order (processus) | commande, raison |
| `RefundIssued` | payment | order (processus), notification | commande, montant, id remboursement |
| `OrderConfirmed` | order | shipping (copie des sous-commandes), payout, promotion, notification | commande, sous-commandes (vendeur, lignes, montant, adresse) |
| `OrderCancelled` | order | promotion (rend la paire d'un drop), notification, analytics | commande, raison (`STOCK_UNAVAILABLE`, `PAYMENT_FAILED`, `PAYMENT_TIMEOUT`, `CUSTOMER_REQUEST`) |
| `ShipmentCreated` | shipping | order (processus), notification | sous-commande, n° de suivi |
| `ShipmentStatusChanged` | shipping | order (affichage du suivi), notification | statut (pris en charge, en transit, en livraison, retard), date estimée |
| `ShipmentDelivered` | shipping | order (processus), payout, notification | sous-commande, date de livraison |
| `SubOrderShipmentOverdue` | order (processus) | notification (relance vendeur) | sous-commande, vendeur, retard |
| `SellerEarningRecorded` | payout | notification | vendeur, sous-commande, brut, commission, net |
| `OrderCompleted` | order | fidélité, analytics | commande, total |

### Cycle de vie

- **Commande :** `PLACED` → `AWAITING_PAYMENT` → `CONFIRMED` → `COMPLETED`, ou `CANCELLED`.
- **Sous-commande :** `CONFIRMED` → `SHIPPED` → `DELIVERED`.

### Décisions

| Sujet | Décision |
|---|---|
| Orchestration | La saga de commande est un **processus BPMN Camunda 8** (`order-fulfillment`). Délais, compensations et attente des colis en parallèle sont visibles dans Camunda Operate. |
| Communication | Le processus publie des **commandes Kafka** ; les réponses (événements) sont **corrélées** au processus par `order-service` (clé : n° de commande ou de sous-commande). Les messages Camunda ont une durée de vie (TTL) pour absorber le cas où la réponse arrive avant que le processus ne l'attende. |
| Prix au checkout | `order-service` garde **sa propre copie des prix** (`ProductVariantAdded` / `ProductPriceChanged`). |
| Paiement | **Stripe Payment Element** (formulaire intégré à la page Angular) + webhooks : c'est nous qui fixons le délai de paiement. |
| Durée de réservation du stock | **30 min** en achat classique, **10 min** pour un drop (variable du processus). |
| Annulation par le client | **Autorisée tant qu'aucun colis n'est parti** : message `CancellationRequested` → remboursement → stock libéré → `OrderCancelled` (`CUSTOMER_REQUEST`). Après expédition : processus de retour (plus tard). |
| Retard vendeur | Si un vendeur n'a pas expédié sous **72 h**, le processus publie `SubOrderShipmentOverdue` (relance). |
| Événements panier | **Publiés** (`CartItemAdded`, …) — détaillés dans le parcours 3. |

---

## Parcours 2 — Drop / vente flash

**Scénario de référence :** samedi 10h00, 300 paires en édition limitée à 120 €, 1 paire par client, 5 000 personnes connectées à 9h59.

### Répartition des rôles techniques

| Outil | Rôle pendant le drop |
|---|---|
| **Redis** | Compteur de stock du drop et file d'attente : opérations atomiques, aucune survente possible. |
| **WebSocket** | Diffusion en direct : compte à rebours, stock restant, position dans la file. |
| **Kafka** | Uniquement les faits métier durables (drop programmé, ouvert, épuisé, paire réservée…), pas chaque clic. |

### Déroulé

```
 AVANT
 Vendeur propose un drop ──▶ promotion-service ══ DropProposed ══▶ notification (admin)
 Admin valide et programme ──▶ promotion ══ DropApproved / DropScheduled ══▶ inventory (met le stock de côté)
 inventory ══ DropStockAllocated ══▶ promotion (charge le compteur Redis)
 T-15 min : promotion ══ DropStartingSoon ══▶ notification

 PENDANT
 Ouverture : promotion ══ DropOpened
 Client ─▶ file d'attente virtuelle (Redis) ─▶ admis par vagues
 Client admis ─▶ choisit sa taille ─▶ Redis : limite par client OK ? stock > 0 ? → décrément atomique
        ✅ promotion ══ DropItemClaimed ─▶ 10 min pour payer
           ─▶ checkout express ─▶ OrderPlaced (avec le n° de réservation) ─▶ saga du parcours 1
        ❌ « Plus disponible dans ta taille »
 Pas payé à temps ─▶ promotion ══ DropClaimExpired ─▶ la paire retourne dans le compteur
 Compteur à 0 ─▶ promotion ══ DropSoldOut

 APRÈS
 Fermeture ─▶ promotion ══ DropClosed ══▶ inventory (les invendus retournent au stock normal)
```

### Événements

| Événement | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `DropProposed` | promotion | notification (admin) | drop, vendeur, SKU et quantités, prix, créneau souhaité |
| `DropApproved` | promotion | notification (vendeur) | drop |
| `DropRejected` | promotion | notification (vendeur) | drop, motif |
| `DropScheduled` | promotion | inventory, catalog (badge « drop à venir »), notification | drop, `mode`, SKU et quantités, prix, ouverture et fermeture, limite par client |
| `DropStockAllocated` | inventory | promotion | drop, quantités réellement mises de côté |
| `DropStartingSoon` | promotion | notification | drop, heure d'ouverture |
| `DropOpened` | promotion | catalog, analytics | drop |
| `DropItemClaimed` | promotion | analytics | drop, réservation, client, SKU, expiration |
| `DropClaimExpired` | promotion | analytics | réservation |
| `DropSoldOut` | promotion | catalog, notification, analytics | drop, SKU |
| `DropClosed` | promotion | inventory (rend les invendus), catalog, analytics | drop, vendus, invendus |

`promotion-service` écoute aussi `OrderConfirmed` (réservation convertie) et `OrderCancelled` (la paire retourne dans le compteur) : la saga du parcours 1 est réutilisée, pas dupliquée.

### Décisions

| Sujet | Décision |
|---|---|
| Création des drops | Les **vendeurs proposent**, l'**admin valide et programme** : processus Camunda `drop-approval` avec tâche humaine dans Tasklist. |
| Paiement | Stripe Payment Element : **10 min** pour payer une paire réservée. |
| Mode | « Premier arrivé, premier servi » avec file d'attente. Champ `mode` prévu dès maintenant (`FCFS`, plus tard `RAFFLE`). |
| Limite par client | **1 par drop** par défaut, configurable par drop. |

---

## Parcours 3 — Achat malin

**Scénario de référence :** Sarah ajoute des Air Max à 160 € à sa wishlist en taille 42 avec un prix cible de 130 €. Le prix passe à 129 € : elle est alertée et achète. Elle laisse aussi un hoodie dans son panier : rappel le lendemain, coupon deux jours plus tard.

### A. Wishlist et alertes (chorégraphie)

```
 Client ─▶ « Ajouter à ma wishlist (taille 42, prix cible 130 €) » ─▶ wishlist ══ WishlistItemAdded ─▶ analytics

 catalog   ══ ProductPriceChanged ══▶ wishlist : qui suit ce produit dans cette taille, avec un prix cible atteint ?
                                          ══ PriceDropAlertTriggered ══▶ notification

 inventory ══ SkuBackInStock ══▶ wishlist : qui attend cette taille ?  (le SKU repasse de 0 à > 0)
                                          ══ BackInStockAlertTriggered ══▶ notification   (par vagues, voir décisions)
 inventory ══ SkuOutOfStock  ══▶ wishlist : arrête les vagues en cours
```

Le dashboard vendeur exploite `WishlistItemAdded` : « 47 personnes attendent ce modèle en 42 ».

### B. Processus BPMN `cart-recovery` (Camunda, workers dans `promotion-service`)

Démarré pour un client **connecté** au premier article ajouté (corrélation par n° de client).

```
 (●) premier CartItemAdded
 <◇ attente> ── OrderPlaced ─────────────▶ (◉ converti)
  ├── le panier change ──▶ retour au début (le délai repart de zéro)
  ├── panier vidé ───────▶ (◉ fin)
  └── ⏱ 24 h ─▶ ══ CartAbandoned ─▶ notification (rappel)
 <◇ attente> ── OrderPlaced ─────────────▶ (◉ converti après relance)
  └── ⏱ 48 h ─▶ [Créer un coupon −5 % valable 48 h] ══ CouponIssued ─▶ notification
 <◇ attente> ── OrderPlaced ─────────────▶ (◉ converti grâce au coupon)
  └── ⏱ 48 h ─▶ (◉ abandon définitif)
```

Les différentes fins du processus permettent de mesurer l'efficacité des relances (phase 8).

### Événements

| Événement | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `CartItemAdded` | cart | promotion (processus), analytics | client, SKU, quantité, prix |
| `CartItemRemoved` | cart | promotion (processus), analytics | client, SKU, nombre d'articles restants |
| `CartAbandoned` | promotion (processus) | notification, analytics | client, articles, valeur du panier |
| `CouponIssued` | promotion | notification | client, code, remise, expiration |
| `WishlistItemAdded` | wishlist | analytics | client, produit, taille, prix cible éventuel |
| `WishlistItemRemoved` | wishlist | analytics | client, produit, taille |
| `SkuBackInStock` | inventory | wishlist | SKU, quantité disponible |
| `SkuOutOfStock` | inventory | wishlist, catalog | SKU |
| `PriceDropAlertTriggered` | wishlist | notification | client, produit, ancien et nouveau prix |
| `BackInStockAlertTriggered` | wishlist | notification | client, SKU, n° de vague |

`cart-service` écoute aussi `OrderPlaced` pour vider le panier.

### Décisions

| Sujet | Décision |
|---|---|
| Propriétaire de la wishlist | **`wishlist-service` dédié** (absorbe seul le flux d'événements de prix et de stock). |
| Prix cible | **Optionnel** : sans prix cible, alerte à chaque baisse ; avec, seulement sous ce prix. |
| Équité du retour en stock | **Par vagues, dans l'ordre d'inscription** : environ 3 clients prévenus par unité disponible ; vague suivante après 1 h si le SKU n'est pas épuisé (`SkuOutOfStock`). Planification interne à `wishlist-service`. |
| Relance de panier | Processus Camunda **`cart-recovery`** dans `promotion-service` : rappel à 24 h, coupon **−5 % valable 48 h** à 72 h, abandon définitif à 120 h. Clients connectés uniquement. |
| Anti-spam | Au plus **une alerte par produit et par client toutes les 24 h**. |
| Consentement | Les relances marketing (panier, coupon) ne partent que si le client a accepté les communications marketing ; règle appliquée par `notification-service`, qui gère les préférences. Les alertes wishlist, demandées par le client, ne sont pas concernées. |

---

## Parcours 4 — Onboarding vendeur

### Processus BPMN `seller-onboarding` (Camunda, workers dans `user-service`)

```
 (●) Demande envoyée ══ SellerApplicationSubmitted ─▶ notification (équipe admin)
 [Vérifications automatiques] : format SIRET (14 chiffres + clé de Luhn), IBAN valide (mod 97), nom de boutique libre
  ├── ❌ échec ─▶ ══ SellerRejected (motif automatique) ─▶ (◉ Refusée)
  ▼
 [👤 Examiner la demande] ← tâche humaine admin (Camunda Tasklist)
  │   ⏱ 48 h sans traitement ─▶ relance de l'équipe admin (non interrompant)
  ├── Refuser ─▶ ══ SellerRejected (motif) ─▶ (◉ Refusée)
  ├── Demander des compléments ─▶ ══ SellerInfoRequested ─▶ le vendeur complète
  │        <attente SellerApplicationUpdated> ─▶ retour à l'examen
  │        └── ⏱ 7 jours sans réponse ─▶ ══ SellerRejected (« dossier incomplet »)
  └── Approuver
       ▼
 [Ouvrir le compte de reversement] ── cmd OpenPayoutAccount (IBAN) ──▶ payout
 <attente PayoutAccountOpened>
 [Attribuer le rôle SELLER dans Keycloak] ─▶ [Créer la boutique]
 ══ SellerApproved ─▶ catalog, promotion, notification
 (◉ Vendeur actif)
```

### Suspension d'un vendeur (action admin)

```
 Admin ─▶ « Suspendre » (motif) ─▶ user ══ SellerSuspended
      ─▶ catalog : masque tous ses produits
      ─▶ promotion : annule ses drops à venir
      ─▶ notification : prévient le vendeur
      (les commandes déjà confirmées continuent normalement)
 Admin ─▶ « Réactiver » ─▶ user ══ SellerReinstated ─▶ catalog : réaffiche ses produits
```

### Commandes et événements

| Événement / commande | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `SellerApplicationSubmitted` | user | notification (admin), analytics | demande, client, nom de boutique |
| `SellerInfoRequested` | user (processus) | notification (vendeur) | demande, informations demandées |
| `SellerApplicationUpdated` | user | user (processus) | demande |
| cmd `OpenPayoutAccount` | user (processus) | payout | vendeur, IBAN |
| `PayoutAccountOpened` | payout | user (processus) | vendeur |
| `SellerApproved` | user (processus) | catalog, promotion, notification, analytics | vendeur, boutique, slug |
| `SellerRejected` | user (processus) | notification | demande, motif |
| `SellerSuspended` | user | catalog, promotion, notification | vendeur, motif |
| `SellerReinstated` | user | catalog, notification | vendeur |

### Décisions

| Sujet | Décision |
|---|---|
| Vérification SIRET / IBAN | **Format uniquement** (clé de Luhn pour le SIRET, mod 97 pour l'IBAN). API Sirene de l'INSEE : idée pour plus tard. |
| Examen des demandes | **Camunda Tasklist** d'abord, puis intégration dans le back-office Angular via l'API Camunda. |
| Demande de compléments | Boucle conservée ; **refus automatique après 7 jours** sans réponse. |
| Relance admin | Timer non interrompant de **48 h** sur la tâche d'examen. |
| Suspension | `SellerSuspended` / `SellerReinstated` : produits masqués, drops à venir annulés, commandes confirmées honorées. |
| Données sensibles | L'IBAN ne transite que par la commande `OpenPayoutAccount` (un seul destinataire), jamais dans un événement. |

---

## Parcours 5 — Vente côté vendeur

### A. Publier un produit

```
 Le vendeur crée « Air Max 90 Infrared » : description, photos (MinIO)  (statut : brouillon)
   ─▶ catalog ══ ProductCreated ══▶ inventory, order, analytics (produit sans variante)
 Il ajoute ses variantes une par une, chacune avec son prix :
 pointures 39 → 46 × 2 coloris = 16 SKU
   ─▶ catalog ══ ProductVariantAdded (×16) ══▶ inventory (stock 0), order (copie des prix)
 Il saisit son stock par taille ─▶ inventory ══ StockAdjusted
 Il publie ─▶ catalog ══ ProductPublished ─▶ visible dans la recherche

 Plus tard :
   nouvelle taille    ─▶ ══ ProductVariantAdded ─▶ inventory, order
   changement de prix ─▶ ══ ProductPriceChanged ─▶ order, wishlist
   stock sous le seuil ─▶ inventory ══ StockLow ─▶ notification (vendeur)
   signalement client ─▶ catalog ══ ProductReported ─▶ notification (admin)
   retrait            ─▶ ══ ProductUnpublished (motif : SELLER, MODERATION, SELLER_SUSPENDED)
```

Le catalogue ne connaît pas le stock ; l'inventaire ne connaît pas les descriptions.

### B. Expédier une commande

```
 OrderConfirmed ─▶ la sous-commande apparaît dans l'espace vendeur
 Le vendeur clique « Remettre au transporteur »
   ─▶ shipping : n° de suivi + étiquette PDF avec code-barres ══ ShipmentCreated
 Transporteur simulé :
   PRIS EN CHARGE ─▶ EN TRANSIT ─▶ (parfois RETARDÉ) ─▶ EN LIVRAISON ─▶ LIVRÉ
   ══ ShipmentStatusChanged (×n) .................... ══ ShipmentDelivered
```

### Événements

| Événement | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `ProductCreated` | catalog | inventory, order, analytics | produit, vendeur, nom, marque, catégorie (le produit naît sans variante) |
| `ProductVariantAdded` | catalog | inventory, order | produit, variante (SKU, taille, coloris, prix) |
| `ProductUpdated` | catalog | analytics | produit, champs modifiés |
| `ProductPublished` | catalog | analytics | produit |
| `ProductUnpublished` | catalog | wishlist, cart (retire l'article), analytics | produit, motif |
| `ProductReported` | catalog | notification (admin), analytics | produit, motif du signalement |
| `StockAdjusted` | inventory | analytics | SKU, variation, motif (réassort, correction) |
| `StockLow` | inventory | notification (vendeur) | SKU, quantité restante, seuil |

### Décisions

| Sujet | Décision |
|---|---|
| Cycle de vie produit | Brouillon → publié → dépublié. |
| Modération | **A posteriori** : publication immédiate, signalement par les clients, dépublication par l'admin (motif `MODERATION`). |
| Seuil de stock bas | 5 unités par défaut, réglable par le vendeur. |
| Colis | **Une sous-commande = un colis** (pas d'envoi fractionné). |
| Transporteur simulé | Vitesse **configurable** : mode démo (≈ 30 s entre deux statuts) ou réaliste (heures/jours) ; **≈ 10 % de retards** aléatoires. |
| Étiquette | **PDF avec code-barres** généré par `shipping-service`. |
| Annulation par le vendeur | Reportée (idées en attente). |

---

## Parcours 6 — Revenus du vendeur

### Grand livre (`payout-service`)

Chaque mouvement est une écriture **immuable** ; une erreur se corrige par une écriture inverse, jamais par une modification.

```
 OrderConfirmed    ─▶ payout mémorise la sous-commande (vendeur, montant)      (rien n'est crédité)
 ShipmentDelivered ─▶ écritures :  + 160,00 €  VENTE        S-42
                                   −  16,00 €  COMMISSION   S-42 (taux 10 % enregistré sur l'écriture)
                    ══ SellerEarningRecorded ─▶ notification, analytics
 J + 14 jours      ─▶ le net passe de « en attente » à « disponible »
 Chaque lundi      ─▶ job Spring Batch : pour chaque vendeur dont le disponible ≥ 20 €
                     ══ PayoutInitiated ─▶ virement SEPA simulé ─▶ ══ PayoutCompleted
                                                              (ou ══ PayoutFailed)
```

Solde vendeur : **en attente** (période de sécurité) → **disponible** (versé au prochain lundi) → **versé**.

### Changement d'IBAN sécurisé

```
 Vendeur ─▶ nouvel IBAN ─▶ payout ── cmd SendTransactionalEmail (lien de confirmation) ──▶ notification
 Vendeur clique le lien ─▶ payout ══ PayoutAccountUpdated (IBAN masqué, versements gelés jusqu'à J+3)
                                    ─▶ notification : alerte de sécurité « Ton IBAN a été modifié »
```

### Commandes et événements

| Événement / commande | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `SellerEarningRecorded` | payout | notification, analytics | vendeur, sous-commande, brut, commission, taux, net, date de disponibilité |
| `PayoutInitiated` | payout | notification, analytics | vendeur, montant, période couverte |
| `PayoutCompleted` | payout | notification | vendeur, montant, référence du virement |
| `PayoutFailed` | payout | notification (vendeur et admin) | vendeur, montant, motif |
| cmd `SendTransactionalEmail` | payout (et tout service ayant un contenu sensible à envoyer) | notification | destinataire, modèle, paramètres (ex. lien de confirmation) |
| `PayoutAccountUpdated` | payout | notification | vendeur, IBAN **masqué** (`FR76 •••• 1234`), gel des versements jusqu'au… |

### Décisions

| Sujet | Décision |
|---|---|
| Crédit vendeur | À la **livraison**, en « en attente ». |
| Disponibilité | **14 jours après la livraison** (délai légal de rétractation). |
| Versements | **Chaque lundi**, à partir de **20 €** disponibles ; en dessous, report à la semaine suivante. |
| Technologie | **Spring Batch** (reprise sur erreur, historique des exécutions, traitement par lots). |
| Commission | Taux **enregistré sur chaque écriture** : un changement de taux ne réécrit pas l'historique. |
| Montants | En **centimes (entiers) + devise** — jamais de nombres à virgule (règle détaillée en 0.3). |
| Changement d'IBAN | **Confirmation par e-mail** + **gel des versements 72 h** + alerte de sécurité. |
| CA plateforme | Somme des commissions, calculée par analytics à partir de `SellerEarningRecorded`. |

---

## Parcours 7 — Avis et fidélité

### A. Avis vérifiés (`review-service`)

```
 OrderConfirmed      ─▶ review mémorise les articles de chaque sous-commande
 ShipmentDelivered   ─▶ le client devient « acheteur vérifié » pour ces produits
 J+3 après livraison ─▶ ══ ReviewRequested ─▶ notification
 Le client publie : note 1-5, texte, photos, taille ressentie (petite / normale / grande)
   ─▶ filtre automatique (insultes, liens, spam)
        ├── OK      ─▶ ══ ReviewPublished ─▶ catalog (note moyenne + synthèse de taille), loyalty
        └── suspect ─▶ en attente ─▶ l'admin publie ou rejette (══ ReviewRejected)
 Signalement par un client ─▶ ══ ReviewReported ─▶ admin ─▶ éventuellement ══ ReviewRemoved
 Réponse publique du vendeur ─▶ ══ ReviewReplied ─▶ notification (auteur de l'avis)
```

Synthèse de taille sur la fiche produit : « 68 % des acheteurs conseillent de prendre une pointure au-dessus ».

### B. Fidélité (`loyalty-service`)

```
 OrderCompleted  ─▶ points en attente (1 pt par €, × multiplicateur du niveau)
 J+14            ─▶ points utilisables
 ReviewPublished avec photo ─▶ + 50 pts
 Seuil franchi   ─▶ ══ LoyaltyTierChanged ─▶ promotion (accès anticipé aux drops), notification
 Conversion 500 pts ─▶ cmd IssueCoupon ─▶ promotion ══ CouponIssued (bon de 5 €) ══ LoyaltyPointsRedeemed
```

| Niveau | Seuil (12 mois glissants) | Avantages |
|---|---|---|
| 🥉 Bronze | 0 pt | 1 pt par € |
| 🥈 Silver | 500 pts | Points × 1,25 |
| 🥇 Gold | 1 500 pts | Points × 1,5 + accès aux drops 15 min avant l'ouverture |

### Commandes et événements

| Événement / commande | Produit par | Écouté par | Données clés |
|---|---|---|---|
| `ReviewRequested` | review | notification | client, produits à évaluer |
| `ReviewPublished` | review | catalog, loyalty, analytics | avis, produit, note, taille ressentie, avec photo |
| `ReviewRejected` | review | notification | avis, motif |
| `ReviewReported` | review | notification (admin) | avis, motif |
| `ReviewRemoved` | review | catalog | avis, produit |
| `ReviewReplied` | review | notification | avis, vendeur |
| `LoyaltyPointsEarned` | loyalty | notification, analytics | client, points, motif, référence |
| `LoyaltyTierChanged` | loyalty | promotion, notification | client, ancien et nouveau niveau |
| `LoyaltyPointsRedeemed` | loyalty | analytics | client, points, coupon |
| cmd `IssueCoupon` | loyalty | promotion | client, montant, motif |

### Décisions

| Sujet | Décision |
|---|---|
| Avis vérifiés | Seuls les acheteurs livrés peuvent noter ; invitation à **J+3**. |
| Taille ressentie | **Oui**, avec synthèse sur la fiche produit. |
| Modération | **Filtre automatique**, admin pour les cas suspects, signalements par les clients. |
| Réponse du vendeur | **Oui**, publique. |
| Barème fidélité | 1 pt/€, +50 pts pour un avis avec photo, Silver 500 pts, Gold 1 500 pts (12 mois glissants), points utilisables à J+14, 500 pts = bon de 5 €. |
| Avantage Gold | **Accès anticipé de 15 min aux drops** (`promotion-service` écoute `LoyaltyTierChanged`). |

---

## Carte récapitulative

**4 processus Camunda · 9 commandes · ≈ 70 événements.** 🆕 = ajouté lors du récapitulatif (validé).

| Service | Processus Camunda | Produit | Consomme |
|---|---|---|---|
| **user** | `seller-onboarding` | 🆕 `CustomerRegistered`, `SellerApplicationSubmitted`, `SellerApplicationUpdated`, `SellerInfoRequested`, `SellerApproved`, `SellerRejected`, `SellerSuspended`, `SellerReinstated` · cmd `OpenPayoutAccount` | `PayoutAccountOpened` |
| **catalog** | — | `ProductCreated`, `ProductVariantAdded`, `ProductUpdated`, `ProductPriceChanged`, `ProductPublished`, `ProductUnpublished`, `ProductReported` | `SellerApproved`, `SellerSuspended`, `SellerReinstated`, `DropScheduled`, 🆕 `DropCancelled`, `DropOpened`, `DropSoldOut`, `DropClosed`, `SkuOutOfStock`, `ReviewPublished`, `ReviewRemoved` |
| **inventory** | — | `StockReserved`, `StockReservationFailed`, `StockReleased`, `StockAdjusted`, `StockLow`, `SkuBackInStock`, `SkuOutOfStock`, `DropStockAllocated` | cmd `ReserveStock`, `ConfirmStockReservation`, `ReleaseStock` · `ProductCreated`, `ProductVariantAdded`, `DropScheduled`, 🆕 `DropCancelled`, `DropClosed` |
| **cart** | — | `CartItemAdded`, `CartItemRemoved` | `OrderPlaced`, `ProductUnpublished` |
| **wishlist** | — | `WishlistItemAdded`, `WishlistItemRemoved`, `PriceDropAlertTriggered`, `BackInStockAlertTriggered` | `ProductPriceChanged`, `ProductUnpublished`, `SkuBackInStock`, `SkuOutOfStock` |
| **order** | `order-fulfillment` | `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `OrderCompleted`, `SubOrderShipmentOverdue` · cmd `ReserveStock`, `CreatePaymentIntent`, `ConfirmStockReservation`, `ReleaseStock`, `CancelPaymentIntent`, `RefundPayment` | `ProductCreated`, `ProductVariantAdded`, `ProductPriceChanged`, 🆕 `DropItemClaimed`, `StockReserved`, `StockReservationFailed`, `PaymentIntentCreated`, `PaymentSucceeded`, `PaymentFailed`, `RefundIssued`, `ShipmentCreated`, `ShipmentStatusChanged`, `ShipmentDelivered` |
| **payment** | — | `PaymentIntentCreated`, `PaymentSucceeded`, `PaymentFailed`, `RefundIssued` | cmd `CreatePaymentIntent`, `CancelPaymentIntent`, `RefundPayment` · webhooks Stripe (HTTP) |
| **shipping** | — | `ShipmentCreated`, `ShipmentStatusChanged`, `ShipmentDelivered` | `OrderConfirmed` |
| **payout** | — | `PayoutAccountOpened`, `SellerEarningRecorded`, `PayoutInitiated`, `PayoutCompleted`, `PayoutFailed`, `PayoutAccountUpdated` · cmd `SendTransactionalEmail` | cmd `OpenPayoutAccount` · `OrderConfirmed`, `ShipmentDelivered` |
| **promotion** | `drop-approval`, `cart-recovery` | `DropProposed`, `DropApproved`, `DropRejected`, `DropScheduled`, 🆕 `DropCancelled`, `DropStartingSoon`, `DropOpened`, `DropItemClaimed`, `DropClaimExpired`, `DropSoldOut`, `DropClosed`, `CartAbandoned`, `CouponIssued` | cmd `IssueCoupon` · `DropStockAllocated`, `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `CartItemAdded`, `CartItemRemoved`, `SellerApproved`, `SellerSuspended`, `LoyaltyTierChanged` |
| **review** | — | `ReviewRequested`, `ReviewPublished`, `ReviewRejected`, `ReviewReported`, `ReviewRemoved`, `ReviewReplied` | `OrderConfirmed`, `ShipmentDelivered` |
| **loyalty** | — | `LoyaltyPointsEarned`, `LoyaltyTierChanged`, `LoyaltyPointsRedeemed` · cmd `IssueCoupon` | 🆕 `CustomerRegistered`, `OrderCompleted`, `ReviewPublished`, `CouponIssued` |
| **notification** | — | — (e-mails, notifications in-app en WebSocket) | cmd `SendTransactionalEmail` · tous les événements qui donnent lieu à un message (voir chaque parcours), dont 🆕 `CustomerRegistered` et 🆕 `DropCancelled` |
| **analytics** | — | (phase 8 : tendances, recommandations) | quasiment tous les événements métier |

### Ajouts du récapitulatif

| Ajout | Pourquoi |
|---|---|
| `DropCancelled` (promotion → inventory, catalog, notification) | La suspension d'un vendeur annule ses drops à venir : il faut rendre le stock mis de côté, retirer le badge « drop à venir » et prévenir les inscrits. |
| `order` consomme `DropItemClaimed` | Au checkout d'un drop, `order-service` doit vérifier que la réservation est valide et appliquer le **prix du drop**, différent du prix catalogue. |
| `CustomerRegistered` (user → loyalty, notification, analytics) | Ouvrir le compte fidélité (Bronze) et envoyer l'e-mail de bienvenue à l'inscription. |
