# Vérification du Jalon V3.0

Ce document décrit les commandes et résultats attendus pour vérifier le backend Git du Config Server et l'architecture. Les vérifications d'exécution doivent être lancées dans des terminaux séparés depuis la racine du projet, dans l'ordre ci-dessous.

## Démarrage

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl client-service spring-boot:run
mvn -pl service-catalog spring-boot:run
mvn -pl appointment-service spring-boot:run
mvn '-pl' 'gateway-service' 'spring-boot:run' '-B' '-Dspring-boot.run.profiles=dynamique'
```

Config Server clone la branche `main` du dépôt GitHub au démarrage et lit `config-repo` (`clone-on-start=true`). Aucun identifiant n'est enregistré dans les fichiers du projet; le dépôt est accessible sans authentification. Pour un dépôt privé, fournir des identifiants via les mécanismes sécurisés de l'environnement (par exemple variables d'environnement injectées par la plateforme), jamais dans les propriétés versionnées.

## URLs et résultats attendus

| Vérification | URL | Résultat attendu |
|---|---|---|
| Config Client properties | `http://localhost:8888/client-service/default` | HTTP 200, nom `client-service`, port 8081, H2/JPA et Eureka |
| Config Catalog properties | `http://localhost:8888/service-catalog/default` | HTTP 200, nom `service-catalog`, port 8082, H2/JPA et Eureka |
| Config Appointment properties | `http://localhost:8888/appointment-service/default` | HTTP 200, nom `appointment-service`, port 8083, H2/JPA et Eureka |
| Config Gateway properties | `http://localhost:8888/gateway-service/default` | HTTP 200, nom `gateway-service`, port 8090 et configuration discovery |
| Eureka | `http://localhost:8761/` | Dashboard accessible; quatre clients apparaissent UP |
| APIs directes | `http://localhost:8081/clients`, `http://localhost:8082/services`, `http://localhost:8083/appointments` | Réponses REST du service correspondant |
| Gateway dynamique | `http://localhost:8090/client-service/clients`, `http://localhost:8090/service-catalog/services`, `http://localhost:8090/appointment-service/appointments` | Réponses des trois APIs via le Gateway |
| Gateway routes | `http://localhost:8090/actuator/gateway/routes` | Routes découverte visibles |
| Health | `/actuator/health` sur les ports 8761, 8888, 8081, 8082, 8083 et 8090 | HTTP 200 et statut UP |

Le Gateway peut aussi être démarré avec le profil `statique` à la place de `dynamique`; un seul profil de routage doit être actif à la fois. La disponibilité réelle dépend du démarrage effectif des applications et doit être constatée dans leurs logs et réponses HTTP.
