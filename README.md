# 👟 ShopFlow

> **La marketplace sneakers & streetwear où des vendeurs indépendants lancent leurs drops, et où chaque achat se vit en temps réel.**

Projet personnel d'architecture **microservices pilotée par les événements** : Angular · Spring Boot · Kafka · MongoDB · Camunda 8 · Axon · Redis · Keycloak · Docker · Kubernetes.

## Fonctionnalités signature

- ⚡ **Drops en direct** : file d'attente virtuelle, stock en temps réel, zéro survente
- 🛰️ **Suivi de commande et de colis en direct**, piloté par un processus BPMN (Camunda)
- 🏪 **Multi-vendeurs** : commandes découpées par vendeur, commission et reversements (Event Sourcing avec Axon)
- 🔔 **Alertes intelligentes** : baisse de prix, retour en stock dans ta pointure
- ⭐ **Avis vérifiés** avec retour sur la taille, 🏆 **fidélité** avec accès anticipé aux drops
- 📊 **Tendances et dashboards temps réel** (Kafka Streams)

## Documentation

| Document | Contenu |
|---|---|
| [Vision](docs/01-vision.md) | Personas, parcours, décisions métier |
| [Event storming](docs/02-event-storming.md) | Processus, commandes et événements de chaque parcours |
| [Conventions](docs/03-conventions.md) | Kafka, Avro, CloudEvents, API REST, erreurs, montants, architecture hexagonale |
| [Architecture](docs/04-architecture.md) | Diagrammes C4 (contexte, conteneurs, zoom commande) |
| [Feuille de route](ROADMAP.md) | Phases, étapes et avancement |

## Démarrage

Prérequis : Docker Desktop, JDK 17+.

```bash
# Infrastructure (Kafka, Schema Registry, MongoDB et leurs interfaces web)
docker compose up -d

# Build du backend (Maven Wrapper : aucune installation de Maven nécessaire)
cd backend
./mvnw install          # Windows : mvnw.cmd install
```

| Outil | URL |
|---|---|
| Kafka UI | http://localhost:8090 |
| Schema Registry | http://localhost:8081 |
| Mongo Express | http://localhost:8091 |
| MongoDB (replica set `rs0`) | `mongodb://localhost:27017/?directConnection=true` |
| Swagger du catalog-service | http://localhost:8101/swagger-ui.html |

## Licence

[MIT](LICENSE)
