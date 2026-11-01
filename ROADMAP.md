# ShopFlow — Feuille de route

> **La marketplace sneakers & streetwear multi-vendeurs, temps réel et pilotée par les événements.**
> Vision détaillée : [docs/01-vision.md](docs/01-vision.md)
> Stack : Angular · Spring Boot · Kafka · MongoDB · Camunda 8 · Axon (+ Redis, Keycloak) · Docker · Kubernetes

## Ce qui rend ShopFlow unique

| Fonctionnalité signature | Ce qu'elle démontre |
|---|---|
| ⚡ **Ventes flash en direct** : compte à rebours, stock qui baisse en temps réel, file d'attente virtuelle, zéro survente | Concurrence, Redis, WebSocket, tests de charge |
| 🛰️ **Suivi de commande live** : chaque étape de la saga s'affiche instantanément | Saga Kafka, SSE/WebSocket |
| 🏪 **Multi-vendeurs** : une commande est découpée en sous-commandes par vendeur | Modélisation métier avancée |
| 🔥 **Tendances & recommandations temps réel** : « en ce moment », « souvent achetés ensemble » | Kafka Streams, fenêtres glissantes |
| 🔔 **Alertes intelligentes** : baisse de prix, retour en stock, stock bas vendeur | Architecture réactive aux événements |
| 🏆 **Programme de fidélité** : points, niveaux Bronze/Silver/Gold, bons de réduction | Consommateurs découplés |
| 📊 **Dashboards vendeur/admin live** : CA à la minute, top ventes | CQRS, read models |

## Architecture cible

| Service | Rôle | Stockage |
|---|---|---|
| `api-gateway` | Point d'entrée, routage, sécurité JWT, rate limiting | Redis |
| `user-service` | Profils, adresses, onboarding vendeurs | MongoDB |
| `catalog-service` | Produits, variantes, catégories, recherche | MongoDB + Redis (cache) |
| `inventory-service` | Stock, réservations, alertes | MongoDB |
| `cart-service` | Panier (invité + connecté) | Redis |
| `wishlist-service` | Wishlist par taille, prix cibles, alertes de prix et de retour en stock | MongoDB |
| `order-service` | Commandes, sous-commandes vendeurs, saga | MongoDB |
| `payment-service` | Paiement Stripe (mode test) via webhooks, remboursements | MongoDB |
| `shipping-service` | Transporteur simulé : n° de suivi, statuts de livraison automatiques | MongoDB |
| `payout-service` | Commission plateforme (10 %), soldes et reversements vendeurs — **Event Sourcing / CQRS avec Axon** | Event store Axon + MongoDB (projections) |
| `promotion-service` | Ventes flash, coupons, règles de prix | MongoDB + Redis |
| `review-service` | Avis vérifiés, modération | MongoDB |
| `loyalty-service` | Points et niveaux de fidélité | MongoDB |
| `notification-service` | E-mail, notifications in-app, préférences | MongoDB |
| `analytics-service` | Tendances, recommandations, statistiques (Kafka Streams) | MongoDB (read models) |
| `frontend` | Espaces client, vendeur et admin (Angular) | — |

Identité : **Keycloak** (rôles `CUSTOMER`, `SELLER`, `ADMIN`).
Processus métier : **Camunda 8** (Zeebe, Operate, Tasklist) — orchestration de la commande, de l'onboarding vendeur et de la validation des drops ; Kafka pour la diffusion des faits.

Légende : ✅ fait · 🟡 en cours · ⬜ à faire · 🖥️ partie frontend

---

## Phase 0 — Conception
- ✅ 0.1 Vision, personas (client, vendeur, admin) et parcours principaux
- ✅ 0.2 Event storming : liste des événements métier et des services qui les produisent ou les consomment
- ✅ 0.3 Conventions : topics Kafka, Avro, CloudEvents, outbox, versioning, API REST, erreurs, montants, identifiants, architecture hexagonale
- ✅ 0.4 Diagrammes d'architecture (C4 : contexte, conteneurs, zoom commande) en Mermaid
- ✅ 0.5 Dépôt Git (GitHub Flow + squash, commits `type(étape): message`), dépôt GitHub public, licence MIT, premier commit

**Livrable :** un dossier `docs/` avec l'architecture et le catalogue d'événements.

## Phase 1 — Socle technique
- ✅ 1.1 Monorepo + projet Maven multi-modules + Maven Wrapper
- ✅ 1.2 Module `common` : événements Kafka partagés *(provisoire, supprimé en 1.4)*
- ✅ 1.3 `docker-compose` infra : Kafka (KRaft), MongoDB, Kafka UI, Mongo Express
- ✅ 1.4 Module `contracts` (Avro `.avsc`, génération des classes, `TopicRecordNameStrategy`, schémas du topic produit + test aller-retour) ; module `shared-kernel` (`Money` métier) ; Schema Registry Confluent dans le compose (compatibilité FULL), branché sur Kafka UI
- ✅ 1.5 Module `shopflow-starter` : enveloppe CloudEvents (headers Kafka), propagation du `correlationId` (HTTP, Kafka, logs), UUID v7 ; MongoDB en replica set (prérequis de l'outbox)
- ✅ 1.6a `shopflow-starter` : configuration Kafka/Avro par défaut + **outbox** (transaction MongoDB, relais réveillé après commit, un seul relais actif, ordre garanti)
- ✅ 1.6b `shopflow-starter` : consommateurs idempotents (transaction MongoDB automatique autour de chaque `@KafkaListener`) + réessais sur place puis DLT par service
- ↪️ 1.6c erreurs Problem Details + règles ArchUnit : réalisées **dans le catalog-service** (3.4 et 3.6), sur du vrai code
- ↪️ 1.7 Redis, Keycloak, Mailpit : ajoutés au compose **au moment où un service en a besoin** (cache du catalogue, phase 2, notifications)
- ↪️ 1.8 Camunda 8 : ajouté avec le premier processus métier (onboarding vendeur ou saga de commande)

**Livrable :** `docker compose up -d` démarre toute l'infra, et le starter est réutilisable par tous les services.

## Phase 2 — Sécurité & identité
- ⬜ 2.1 Keycloak : realm `shopflow`, clients, rôles, utilisateurs de test (import automatique)
- ⬜ 2.2 `api-gateway` : routage, validation JWT, relais du token, CORS
- ⬜ 2.3 Services en resource server OAuth2 et autorisations par rôle
- ⬜ 2.4 `user-service` : profil, carnet d'adresses, événement `user.registered`
- ⬜ 2.5 Onboarding vendeur : processus Camunda `seller-onboarding` (vérifications automatiques, tâche humaine admin dans Tasklist, relance après 48 h, demande de compléments), événement `SellerApproved`
- ⬜ 2.6 Suspension / réactivation d'un vendeur (`SellerSuspended`, `SellerReinstated`)
- ⬜ 2.7 🖥️ Socle Angular : architecture (standalone, signals, lazy loading), design system, connexion Keycloak (OIDC), guards par rôle, layout client/vendeur/admin

**Livrable :** on se connecte et chaque rôle voit son espace.

## Phase 3 — Catalogue & recherche
*Étapes courtes : chacune donne un résultat visible. En attendant Keycloak (phase 2), le vendeur est identifié par l'en-tête `X-Seller-Id`.*

- ✅ 3.1 Squelette du `catalog-service` : démarre, se connecte à MongoDB et Kafka, socle du starter actif (`/actuator/health`)
- ✅ 3.2 Domaine Produit en Java pur : brouillon et règles de base (tests unitaires)
- ✅ 3.3a Cas d'usage « créer un produit » + enregistrement dans MongoDB (port `ProductRepository`, adaptateur Mongo)
- ✅ 3.3b API REST créer + consulter un produit + fichier `.http`
- ✅ 3.3c Outillage : Lombok + MapStruct configurés, conventions « Code Java » écrites
- ✅ 3.3d Starter et shared-kernel : `record` → classes Lombok
- ✅ 3.3e catalog-service, domaine + application : classes Lombok, `application/api` (cas d'usage) / `spi` / `service`, `CreateProductCommand`, tests GIVEN/WHEN/THEN
- ✅ 3.3f catalog-service, web : DTO `…RequestDto`/`…ResponseDto`, mapper MapStruct, Bean Validation, test `@WebMvcTest`
- ✅ 3.3g catalog-service, MongoDB : `ProductDocument` en classe, mapper MapStruct
- ✅ 3.3h Swagger (springdoc-openapi) : page Swagger UI, interfaces `…ControllerDocumentation`
- ✅ 3.4a Socle des erreurs Problem Details (RFC 9457) : `BusinessException` (`shared-kernel`) + gestionnaire automatique (starter)
- ✅ 3.4b Erreurs métier du catalogue (`PRODUCT_NOT_FOUND`, `INVALID_PRODUCT`…), documentées dans Swagger
- ✅ 3.5 Événement `ProductCreated` publié via l'outbox (visible dans Kafka UI)
- ✅ 3.6 Règles ArchUnit (architecture hexagonale, conventions ShopFlow, hygiène) dans le module `shopflow-test-support`, appliquées au catalogue
- ✅ 3.7 Variantes (pointure × coloris, SKU, prix) + `ProductVariantAdded`
- ⬜ 3.8 Modifier un produit, changer un prix (`ProductUpdated`, `ProductPriceChanged`)
- ⬜ 3.9 Publier / dépublier (`ProductPublished`, `ProductUnpublished`), pas de publication sans variante
- ⬜ 3.10 Lister ses produits (pagination, filtre par statut)
- ⬜ 3.11 Catégories arborescentes
- ⬜ 3.12 Images produits (stockage MinIO, compatible S3)
- ⬜ 3.13 Recherche full-text + filtres à facettes + tri + pagination
- ⬜ 3.14 Cache Redis des produits populaires et invalidation par événement
- ⬜ 3.15 Signalement d'un produit par les clients + dépublication par l'admin (`ProductReported`)
- ⬜ 3.16 🖥️ Page catalogue, recherche à facettes, fiche produit, gestion produits côté vendeur

**Livrable :** un vendeur publie un produit, un client le trouve via la recherche.

## Phase 4 — Stock
- ⬜ 4.1 `inventory-service` : stock par variante (SKU), initialisé par `product.created`
- ⬜ 4.2 Réservation **atomique** (pas de survente même en concurrence)
- ⬜ 4.3 Expiration automatique des réservations non payées (30 min en classique, 10 min pour un drop)
- ⬜ 4.4 Événements `StockLow` (pour le vendeur), `SkuBackInStock` et `SkuOutOfStock` (pour les alertes)
- ⬜ 4.5 🖥️ Gestion du stock côté vendeur, badges de stock côté client

**Livrable :** le stock est fiable et réagit aux événements.

## Phase 5 — Panier, commandes & paiement (cœur de la saga)
- ⬜ 5.1 `cart-service` (Redis) : panier invité, fusion à la connexion, expiration
- ⬜ 5.2 `order-service` : création de commande, découpage en sous-commandes par vendeur
- ⬜ 5.3 Processus BPMN `order-fulfillment` (Camunda) : workers qui publient les commandes Kafka, corrélation des événements de réponse, timers, compensations
- ✅ 5.4 **Transactional Outbox** : aucun événement perdu entre MongoDB et Kafka *(fait en 1.6a, dans le starter)*
- ✅ 5.5 Réessais sur place + Dead Letter Topic, consommateurs idempotents *(fait en 1.6b, dans le starter)*
- ⬜ 5.6 `payment-service` : Stripe Payment Element en mode test + réception des webhooks (paiement asynchrone)
- ⬜ 5.7 Annulation et remboursement Stripe (transactions de compensation)
- ⬜ 5.8 `shipping-service` : remise au transporteur, étiquette PDF avec code-barres, transporteur simulé à vitesse configurable (démo / réaliste), ≈ 10 % de retards
- ⬜ 5.9 `payout-service` en **Event Sourcing / CQRS avec Axon Framework** (événements de domaine privés, événements d'intégration publiés sur Kafka) : grand livre immuable, commission de 10 % à la livraison, disponibilité à J+14, versements chaque lundi (≥ 20 €) avec **Spring Batch**, changement d'IBAN sécurisé (confirmation e-mail + gel 72 h)
- ⬜ 5.10 🖥️ Panier, checkout avec formulaire Stripe intégré, **suivi de commande et de colis en direct**, historique, commandes et revenus côté vendeur

**Livrable :** une commande traverse toute la saga, y compris les cas d'échec, visibles en direct.

## Phase 6 — Fonctionnalités signature temps réel
- ⬜ 6.1 `promotion-service` : coupons (pourcentage, montant fixe, conditions, limite d'utilisation)
- ⬜ 6.2 **Drops** : proposés par les vendeurs, validés et programmés par l'admin (processus Camunda `drop-approval`), stock dédié, ouverture et fermeture automatiques, champ `mode` (raffle plus tard)
- ⬜ 6.3 File d'attente virtuelle + compteur de stock atomique + limite par client (1 par défaut) dans Redis
- ⬜ 6.4 Rate limiting à la gateway et protection anti-bot sur les ventes flash
- ⬜ 6.5 Diffusion temps réel (WebSocket) : compte à rebours, stock restant, « X personnes regardent »
- ⬜ 6.6 `wishlist-service` : wishlist par taille, prix cible, alertes de baisse de prix et de retour en stock (par vagues)
- ⬜ 6.7 Processus Camunda `cart-recovery` : rappel à 24 h, coupon −5 % à 72 h
- ⬜ 6.8 🖥️ Page vente flash live, saisie de coupon au checkout, wishlist, création de ventes flash côté admin

**Livrable :** une vente flash où des centaines de clients achètent en même temps, sans survente.

## Phase 7 — Engagement client
- ⬜ 7.1 `review-service` : avis réservés aux acheteurs vérifiés (invitation à J+3), photos, **taille ressentie**, votes « utile », réponse publique du vendeur
- ⬜ 7.2 Modération (filtre automatique + admin pour les cas suspects + signalements) ; note moyenne et synthèse de taille recalculées dans le catalogue
- ⬜ 7.3 `loyalty-service` : 1 pt/€ (utilisable à J+14), bonus avis photo, niveaux Bronze/Silver/Gold sur 12 mois glissants, 500 pts = 5 €, **accès anticipé aux drops pour les Gold**
- ⬜ 7.4 `notification-service` : templates d'e-mails (Mailpit), notifications in-app, préférences
- ⬜ 7.5 🖥️ Avis sur la fiche produit, page fidélité, centre de notifications

**Livrable :** l'expérience client est complète, de la découverte à la fidélisation.

## Phase 8 — Data & intelligence
- ⬜ 8.1 Tracking des événements de navigation (`product.viewed`, `search.performed`)
- ⬜ 8.2 `analytics-service` (Kafka Streams) : CA en temps réel, top ventes, fenêtres glissantes
- ⬜ 8.3 Produits tendance (« 🔥 en ce moment »)
- ⬜ 8.4 Recommandations : « souvent achetés ensemble », « vus récemment »
- ⬜ 8.5 Read models CQRS pour les dashboards
- ⬜ 8.6 🖥️ Dashboard vendeur, dashboard admin (graphiques live), widgets de recommandation

**Livrable :** des tableaux de bord qui bougent en direct pendant une vente flash.

## Phase 9 — Qualité & tests
- ⬜ 9.1 Tests unitaires (JUnit 5, Mockito) + couverture JaCoCo
- ⬜ 9.2 Tests d'intégration avec Testcontainers (MongoDB, Kafka, Redis)
- ⬜ 9.3 Tests de contrat entre services (Spring Cloud Contract)
- ⬜ 9.4 Tests frontend + tests end-to-end (Playwright)
- ⬜ 9.5 Tests de charge sur la vente flash (k6 ou Gatling)
- ⬜ 9.6 Analyse statique (SonarQube)

## Phase 10 — Résilience & observabilité
- ⬜ 10.1 Resilience4j : circuit breaker, retry, timeouts sur les appels synchrones
- ⬜ 10.2 Métriques Micrometer + Prometheus + dashboards Grafana (techniques et métier : commandes/min, lag Kafka)
- ⬜ 10.3 Traces distribuées OpenTelemetry (y compris à travers Kafka) + Tempo/Jaeger
- ⬜ 10.4 Logs JSON centralisés (Loki) avec `correlationId`
- ⬜ 10.5 Alerting (Alertmanager) : erreurs, lag consommateur, DLT non vide
- ⬜ 10.6 Documentation des API : OpenAPI (springdoc, agrégé dans la gateway) + AsyncAPI pour les événements

## Phase 11 — Conteneurisation & CI/CD
- ⬜ 11.1 Dockerfiles multi-stage optimisés (non-root, couches) pour chaque service + frontend (nginx)
- ⬜ 11.2 `docker-compose` complet : toute la stack en une commande
- ⬜ 11.3 GitHub Actions — CI : build, tests, couverture, Sonar sur chaque PR
- ⬜ 11.4 GitHub Actions — CD : build des images, scan de sécurité (Trivy), push sur GHCR, versioning sémantique
- ⬜ 11.5 Mises à jour automatiques des dépendances (Dependabot ou Renovate)

## Phase 12 — Déploiement Kubernetes
- ⬜ 12.1 Cluster local (kind / Docker Desktop) + premiers manifests
- ⬜ 12.2 Chart Helm ShopFlow (un sous-chart par service, valeurs par environnement)
- ⬜ 12.3 Kafka via l'opérateur Strimzi, MongoDB en replica set, Redis
- ⬜ 12.4 Ingress NGINX + TLS avec cert-manager
- ⬜ 12.5 Secrets (Sealed Secrets ou External Secrets), ConfigMaps
- ⬜ 12.6 Probes, requests/limits, autoscaling HPA (démontré pendant une vente flash)
- ⬜ 12.7 GitOps avec Argo CD : environnements dev / staging / prod
- ⬜ 12.8 Déploiement cloud + infrastructure as code (Terraform)
- ⬜ 12.9 Stack d'observabilité sur le cluster (kube-prometheus-stack, Loki, Tempo)

## Phase 13 — Finitions & portfolio
- ⬜ 13.1 Jeu de données de démo réaliste (vendeurs, produits, avis)
- ⬜ 13.2 README complet, architecture, captures d'écran, guide de démarrage
- ⬜ 13.3 ADR (Architecture Decision Records) des choix importants (dont : orchestration Camunda vs chorégraphie, Event Sourcing limité à `payout-service`)
- ⬜ 13.4 Scénarios de démo + vidéo de présentation
