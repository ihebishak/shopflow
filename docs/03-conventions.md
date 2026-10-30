# 03 — Conventions

## 1. Kafka

### 1.1 Topics

- **Un topic par entité** ; la **clé du message est l'identifiant de l'entité**. Tous les événements d'une même entité arrivent dans la même partition, donc **dans l'ordre**.
- **Un seul service producteur par topic** (son propriétaire).
- Nommage : minuscules, séparateur `.` uniquement (jamais `_`) :
  - événements : `shopflow.<entité>.events`
  - commandes : `shopflow.<service destinataire>.commands`
- Messages en échec : une DLT **par service consommateur**, `<topic>.<service>.dlt` (ex. `shopflow.order.events.inventory-service.dlt`). Pas de topics de réessai : ils casseraient l'ordre (voir § 1.4).

| Topic | Propriétaire | Clé | Événements |
|---|---|---|---|
| `shopflow.customer.events` | user | customerId | `CustomerRegistered` |
| `shopflow.seller.events` | user | sellerId | `SellerApplicationSubmitted`, `SellerApplicationUpdated`, `SellerInfoRequested`, `SellerApproved`, `SellerRejected`, `SellerSuspended`, `SellerReinstated` |
| `shopflow.product.events` | catalog | productId | `ProductCreated`, `ProductVariantAdded`, `ProductUpdated`, `ProductPriceChanged`, `ProductPublished`, `ProductUnpublished`, `ProductReported` |
| `shopflow.stock.events` | inventory | sku | `StockAdjusted`, `StockLow`, `SkuBackInStock`, `SkuOutOfStock` |
| `shopflow.reservation.events` | inventory | orderId / dropId | `StockReserved`, `StockReservationFailed`, `StockReleased`, `DropStockAllocated` |
| `shopflow.cart.events` | cart | customerId | `CartItemAdded`, `CartItemRemoved` |
| `shopflow.wishlist.events` | wishlist | customerId | `WishlistItemAdded`, `WishlistItemRemoved`, `PriceDropAlertTriggered`, `BackInStockAlertTriggered` |
| `shopflow.order.events` | order | orderId | `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `OrderCompleted`, `SubOrderShipmentOverdue` |
| `shopflow.payment.events` | payment | orderId | `PaymentIntentCreated`, `PaymentSucceeded`, `PaymentFailed`, `RefundIssued` |
| `shopflow.shipment.events` | shipping | subOrderId | `ShipmentCreated`, `ShipmentStatusChanged`, `ShipmentDelivered` |
| `shopflow.payout.events` | payout | sellerId | `PayoutAccountOpened`, `SellerEarningRecorded`, `PayoutInitiated`, `PayoutCompleted`, `PayoutFailed`, `PayoutAccountUpdated` |
| `shopflow.drop.events` | promotion | dropId | `DropProposed`, `DropApproved`, `DropRejected`, `DropScheduled`, `DropCancelled`, `DropStartingSoon`, `DropOpened`, `DropItemClaimed`, `DropClaimExpired`, `DropSoldOut`, `DropClosed` |
| `shopflow.marketing.events` | promotion | customerId | `CartAbandoned`, `CouponIssued` |
| `shopflow.review.events` | review | productId | `ReviewRequested`, `ReviewPublished`, `ReviewRejected`, `ReviewReported`, `ReviewRemoved`, `ReviewReplied` |
| `shopflow.loyalty.events` | loyalty | customerId | `LoyaltyPointsEarned`, `LoyaltyTierChanged`, `LoyaltyPointsRedeemed` |

| Topic de commandes | Destinataire | Clé | Commandes |
|---|---|---|---|
| `shopflow.inventory.commands` | inventory | orderId | `ReserveStock`, `ConfirmStockReservation`, `ReleaseStock` |
| `shopflow.payment.commands` | payment | orderId | `CreatePaymentIntent`, `CancelPaymentIntent`, `RefundPayment` |
| `shopflow.payout.commands` | payout | sellerId | `OpenPayoutAccount` |
| `shopflow.promotion.commands` | promotion | customerId | `IssueCoupon` |
| `shopflow.notification.commands` | notification | id du destinataire | `SendTransactionalEmail` |

### 1.2 Format : Avro + Schema Registry

- Chaque événement et chaque commande a un **schéma Avro** enregistré dans le **Schema Registry** (Confluent).
- Les schémas sont écrits en **`.avsc`** (le format natif d'Avro, identique à ce que stocke le registre), **un fichier par type**, rangés par topic : `src/main/avro/<entité>/<Type>.avsc` dans le module Maven **`contracts`** ; les classes Java sont **générées** au build (`stringType = String`).
- Types partagés (`Money`…) : `src/main/avro/common/`. Tout type référencé par un autre schéma est déclaré dans les `<imports>` du plugin Avro (`contracts/pom.xml`) pour être compilé en premier.
- Version d'un événement : propriété `"version"` dans le schéma (absente = 1), reprise dans le header `ce_type` (`….v2`).
- **Stratégie de nommage des subjects : `TopicRecordNameStrategy`** (`<topic>-<nom complet du record>`). Indispensable avec « un topic par entité » : chaque type d'événement d'un topic a son propre subject et sa propre vérification de compatibilité.
- Les **enums** ont une valeur de repli (`"default": "UNKNOWN"`) pour qu'un consommateur plus ancien supporte une valeur ajoutée plus tard.
- Côté consommateur : `specific.avro.reader = true` → chaque message arrive directement sous la forme de sa classe générée.
- Kafka UI est branché sur le Schema Registry pour décoder les messages.
- Les événements de domaine **internes** à `payout-service` (Axon) ne sont pas concernés : ils restent privés au service.

### 1.3 Enveloppe : CloudEvents (headers Kafka)

Le message contient uniquement les données métier ; les métadonnées sont dans les headers, selon le standard **CloudEvents** (mode binaire) :

| Header | Exemple | Rôle |
|---|---|---|
| `ce_specversion` | `1.0` | Version du standard |
| `ce_id` | UUID | Identifiant unique du message → idempotence |
| `ce_type` | `shopflow.order.OrderPlaced.v1` | Type et version de l'événement ou de la commande |
| `ce_source` | `/order-service` | Producteur |
| `ce_time` | `2026-10-28T10:00:00Z` | Date du fait (UTC) |
| `ce_subject` | id de l'entité | Entité concernée |
| `ce_correlationid` | UUID | Identifiant du parcours de bout en bout (services, Camunda, logs, traces) |
| `ce_causationid` | UUID | `ce_id` du message qui a provoqué celui-ci |

**Configuration par défaut (module `shopflow-starter`)** : sérialiseurs Avro, URL du Schema Registry (`http://localhost:8081`), `TopicRecordNameStrategy`, lecture en classes générées, `acks=all` + idempotence du producteur, groupe de consommateurs = nom du service. Un service n'a besoin que de `spring.application.name`.
**Enregistrement des schémas** : automatique en local (`auto.register.schemas=true`) ; en CI et production, `SPRING_KAFKA_PROPERTIES_AUTO_REGISTER_SCHEMAS=false` : seule la CI enregistre les schémas après vérification de compatibilité.

**Mise en œuvre (module `shopflow-starter`)** — rien à écrire dans les services :

- Publication : `KafkaEventPublisher.publish(topic, clé, message)` calcule l'enveloppe (`ce_type` déduit de la classe Avro, `ce_source` = `/` + `spring.application.name`, `ce_id` en UUID v7…).
- Consommation : un intercepteur restaure la corrélation dans le MDC et mémorise le message en cours ; tout message publié pendant son traitement reçoit automatiquement le même `ce_correlationid` et `ce_causationid` = son `ce_id`.
- HTTP : header **`X-Correlation-Id`** (créé par la gateway, repris ou généré par chaque service, renvoyé dans la réponse ; valeur extérieure filtrée : `[A-Za-z0-9-]{1,64}`).
- Logs : chaque ligne affiche `[correlationId]` après le niveau de log.
- Le `correlationId` (parcours métier, peut durer des jours dans Camunda) est distinct du `traceId` OpenTelemetry (phase 10, requête technique de quelques secondes).

### 1.4 Garanties de livraison

- **Outbox systématique** : l'état métier et l'événement à publier sont écrits dans **la même transaction MongoDB** ; un relais publie ensuite sur Kafka.
  - Dans un service : `outboxPublisher.publish(topic, clé, message)` dans une méthode `@Transactional` (hors transaction → `IllegalStateException`).
  - Collection `outbox` : contenu en **JSON** (lisible dans Mongo Express), enveloppe CloudEvents, `publishedAt`, `attempts`, `lastError`. Les messages publiés sont supprimés après **7 jours** (index TTL).
  - Relais **réveillé dès la validation de la transaction**, plus un passage de sécurité **chaque seconde**.
  - **Un seul relais actif** par service (verrou `outbox_lock`) ; publication **dans l'ordre** des `ce_id` (UUID v7 strictement croissants) ; **arrêt au premier échec**, reprise au passage suivant.
  - Réglages : `shopflow.outbox.poll-interval`, `batch-size`, `lock-lease`, `send-timeout`, `retention`.
  - **Délais** : un envoi bloqué ne doit jamais survivre au verrou, sinon un second relais publierait en parallèle. Le verrou est renouvelé avant chaque message ; Kafka abandonne avant le relais (`delivery.timeout.ms` 10 s < `send-timeout` 15 s) ; l'envoi complet tient dans le verrou (`max.block.ms` 10 s + `send-timeout` 15 s < `lock-lease` 30 s). Vérifié au démarrage : un réglage incohérent empêche le service de démarrer.
  - MongoDB tourne en **replica set** `rs0` (un seul nœud en local). Depuis la machine hôte : `mongodb://localhost:27017/?directConnection=true` ; entre conteneurs : `mongodb://mongo:27017/?replicaSet=rs0`.
- **Consommateurs idempotents** : chaque consommateur mémorise les `ce_id` traités et ignore les doublons (livraison « au moins une fois »).
  - Automatique dans les services MongoDB : le starter enveloppe chaque `@KafkaListener` dans une **transaction MongoDB** (pas besoin de `@Transactional`) ; traitement, marqueur `processed_messages` et outbox : tout ou rien.
  - Marqueurs supprimés après **7 jours** (`shopflow.consumer.processed-retention`, au moins la rétention Kafka).
  - Limite : ne protège que les écritures MongoDB ; les effets externes (e-mail, Stripe…) ont leur propre protection (clé d'idempotence).
- **Erreurs** : réessais **sur place** (les messages suivants attendent : l'ordre est préservé), 4 réessais après 1 s, 2 s, 4 s, 8 s (`shopflow.consumer.retries`, `initial-interval`, `multiplier`), puis mise de côté dans la DLT du service et passage au message suivant. Surveillance et alertes en phase 10.
  - Erreurs définitives (message illisible, mauvais type) : directement en DLT, sans réessai. Un message illisible y est copié en octets bruts (`ErrorHandlingDeserializer`).
  - Le message en DLT garde sa clé, son contenu et son enveloppe CloudEvents ; en-têtes ajoutés : `kafka_dlt-exception-message`, `kafka_dlt-original-topic`, `-partition`, `-offset`, `-consumer-group`.
  - Schémas : le producteur de la DLT enregistre lui-même le schéma pour le topic DLT (copie d'un schéma déjà validé par la CI), seule exception à « seule la CI enregistre ».

### 1.5 Versioning

- Compatibilité **FULL** dans le Schema Registry : on ne peut qu'**ajouter ou retirer des champs optionnels avec valeur par défaut**.
- Changement incompatible → **nouveau type** (`….v2`) publié en parallèle de la v1 le temps que tous les consommateurs migrent.

## 2. API REST

- Tout passe par la **gateway**, préfixe versionné **`/api/v1`**.
- **Trois espaces** (sécurisés par chemin dans la gateway) :

| Espace | Préfixe | Exemples |
|---|---|---|
| Client / public | `/api/v1/…` | `/api/v1/products`, `/api/v1/orders` |
| Vendeur | `/api/v1/seller/…` | `/api/v1/seller/products`, `/api/v1/seller/sub-orders` |
| Admin | `/api/v1/admin/…` | `/api/v1/admin/sellers`, `/api/v1/admin/drops` |

- Ressources **au pluriel**, URL en **kebab-case**, JSON en **camelCase**.
- Actions métier : **`POST` sur un verbe explicite** — `POST /api/v1/orders/{id}/cancel`, `POST /api/v1/admin/sellers/{id}/suspend`.
- Pagination style Spring : `?page=0&size=20&sort=createdAt,desc`.
- Documentation **OpenAPI** générée (springdoc), agrégée dans la gateway.

## 3. Erreurs : Problem Details (RFC 9457)

Toutes les erreurs de tous les services ont la même forme (`application/problem+json`) :

```json
{
  "type": "https://shopflow.dev/errors/order-already-shipped",
  "title": "Order already shipped",
  "status": 409,
  "detail": "Order ORD-8F3K2Q cannot be cancelled: sub-order ORD-8F3K2Q-1 has been shipped.",
  "code": "ORDER_ALREADY_SHIPPED",
  "correlationId": "4f1c…"
}
```

- `code` : **stable**, en `UPPER_SNAKE_CASE` ; le frontend le traduit en français.
- `correlationId` : permet de retrouver toute la trace dans les logs.
- Erreurs de validation : liste `errors[]` (`field`, `code`, `message`).

## 4. Montants

- Partout (API, événements, MongoDB) : **`{ "amount": 16000, "currency": "EUR" }`** — montant en **centimes** (entier) + devise ISO 4217.
- **Deux représentations**, pour respecter l'architecture hexagonale :
  - **`Money` métier** (`com.shopflow.kernel.money.Money`, module **`shared-kernel`**, Java pur) : tous les calculs (addition, multiplication, pourcentage arrondi) ; utilisé par les domaines.
  - **`Money` Avro** (`com.shopflow.contracts.common.Money`, module `contracts`) : transport uniquement ; conversion dans les adaptateurs d'infrastructure.
- Jamais de `double`/`float` pour de l'argent ; dépassement de capacité → exception (jamais de valeur silencieusement fausse).
- Pourcentages exprimés en **points de base** (1 000 = 10 %).
- **Commission arrondie au centime inférieur** (le vendeur n'est jamais lésé) : 10 % de 159,99 € = 15,99 €.

## 5. Identifiants

- **Technique : UUID v7** (unique et ordonné dans le temps, efficace pour les index MongoDB).
- **Références lisibles** pour les humains (e-mails, support, interface) :

| Objet | Format | Exemple |
|---|---|---|
| Commande | `ORD-` + 6 caractères (base 32 sans ambiguïté) | `ORD-8F3K2Q` |
| Sous-commande | commande + numéro | `ORD-8F3K2Q-1` |
| Colis | `SF` + 9 chiffres + `FR` | `SF482913570FR` |
| Drop | `DRP-` + 6 caractères | `DRP-K29XQA` |
| Versement | `PAY-` + 6 caractères | `PAY-7HD2MF` |
| Boutique | slug | `karim-streetwear` |

## 6. Dates et durées

- Dates **toujours en UTC**, format ISO-8601 (`Instant` en Java) ; conversion en heure de Paris **uniquement dans l'interface**.
- Durées en ISO-8601 (`PT30M`, `P14D`), aussi bien dans la configuration que dans les timers BPMN.

## 7. Langue

| En anglais | En français |
|---|---|
| Code, API, événements, topics, codes d'erreur, messages de commit | Documentation, interface (traduisible en anglais plus tard) |

## 8. Architecture interne d'un service : hexagonale « légère »

```
com.shopflow.<service>
├── domain                 modèle et règles métier — Java pur, sans Spring, Kafka, MongoDB ni Camunda
├── application
│   ├── api                ports d'entrée : ce que le service offre (CreateProductUseCase) + commandes
│   ├── spi                ports de sortie : ce dont il a besoin (ProductRepository)
│   └── service            implémentation des cas d'usage
└── infrastructure         adaptateurs
    ├── web
    │   ├── controller     contrôleurs REST
    │   ├── swagger        interfaces de documentation OpenAPI (…ControllerDocumentation)
    │   ├── dto/request    corps reçus (…RequestDto)
    │   ├── dto/response   corps renvoyés (…ResponseDto)
    │   ├── mapper         mappers MapStruct DTO ↔ application
    │   └── advice         @RestControllerAdvice (erreurs → Problem Details)
    ├── mongo              documents, mappers, implémentations des ports spi
    └── kafka, camunda, stripe…
```

- Sens des dépendances : `infrastructure` → `application` → `domain`, **jamais l'inverse**.
- `application` peut utiliser Spring (`@Service`, `@Transactional`), **jamais** MongoDB, Kafka ni le web.
- Règles vérifiées automatiquement par des **tests ArchUnit** (règles partagées via le starter).
- Camunda et Axon sont des **détails d'infrastructure** : le domaine ne les connaît pas.

## 9. Code Java

**Classes**
- **Pas de `record`** : des classes, aux champs `private final` (immuables autant que possible).
- **Lombok** pour le code répétitif :
  - `@Getter` (accesseurs `getName()`) ; `@RequiredArgsConstructor` pour l'injection par constructeur ;
  - `@Builder` + `@Jacksonized` pour les DTO ; `@EqualsAndHashCode` + `@ToString` pour les objets comparés par valeur (identifiants, montants) ;
  - `@Slf4j` pour les logs ; jamais `@Data` ni `@Setter` (pas de mutation libre).
- Le domaine garde des **constructeurs privés** et des méthodes de fabrique qui vérifient les règles (`Product.draft(...)`).
- Injection **par constructeur** uniquement (jamais `@Autowired` sur un champ) ; paramètres de méthodes publiques en **`final`** dans les adaptateurs web.

**API REST**
- DTO suffixés **`RequestDto`** (entrée) et **`ResponseDto`** (sortie), sans logique.
- **Bean Validation** sur les `RequestDto` (`@NotBlank`, `@Size`… avec un message) et `@Valid` dans le contrôleur ; le domaine garde ses propres règles (dernière ligne de défense).
- Le contrôleur implémente une interface **`…ControllerDocumentation`** qui porte les annotations OpenAPI (`@Tag`, `@Operation`) : le contrôleur reste lisible.
- `consumes` / `produces` explicites (`application/json`) ; `@ResponseStatus` pour le code de succès, `ResponseEntity` seulement quand il faut des en-têtes (ex. `Location`).

**Mappers**
- **MapStruct** (interfaces, code généré à la compilation, injectables par Spring) : `…RestMapper` (web) et `…DocumentMapper` (MongoDB).
- `unmappedTargetPolicy = ERROR` (configuré dans le `pom.xml` parent) : un champ oublié fait échouer la compilation.

**Tests**
- `@DisplayName("SHOULD … WHEN …")` et blocs `// GIVEN`, `// WHEN`, `// THEN`.
- Domaine et cas d'usage : tests unitaires sans Spring ; contrôleurs : `@WebMvcTest` avec des cas d'usage simulés ; adaptateurs : vrais MongoDB / Kafka (Testcontainers, Kafka embarqué).
