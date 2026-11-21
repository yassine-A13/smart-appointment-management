# Smart Appointment Management

See [TESTING_REPORT.md](TESTING_REPORT.md) for infrastructure verification commands and expected results.

## TP4 — Communication synchrone avec OpenFeign

Appointment Service utilise deux clients OpenFeign pour joindre directement les services métier :

```text
AppointmentService
  ├─ ClientsRequest (@FeignClient("client-service"))
  │    └─ Eureka + Spring Cloud LoadBalancer → Client Service
  └─ ServiceRequest (@FeignClient("service-catalog"))
       └─ Eureka + Spring Cloud LoadBalancer → Service Catalog
```

Les clients Feign utilisent les noms d’application Eureka (`client-service` et `service-catalog`) et les chemins REST des contrôleurs (`/clients/{id}` et `/services/{id}`). Les appels sont synchrones et ne passent pas par API Gateway. Eureka fournit les instances disponibles; Spring Cloud LoadBalancer en sélectionne une.

Lors d’une création, Appointment Service valide d’abord l’en-tête d’idempotence et recherche une clé déjà traitée. Ensuite, il récupère le client et le service par Feign, vérifie que le service est disponible, puis enregistre le rendez-vous avec le statut initial `PENDING`. Client Service ne possède pas d’indicateur d’activation.

### Endpoints Appointment Service

| Méthode | Endpoint | Résultat |
|---|---|---|
| `POST` | `/appointments` | Crée un rendez-vous, HTTP 201; exige l’en-tête `Idempotency-Key` |
| `GET` | `/appointments` | Liste les rendez-vous |
| `GET` | `/appointments/{id}` | Consultation locale |
| `GET` | `/appointments/{id}/detail` | Rendez-vous enrichi via Client Service et Service Catalog, HTTP 200 |
| `PUT` | `/appointments/{id}` | Modifie un rendez-vous |
| `PATCH` | `/appointments/{id}/cancel` | Annule un rendez-vous |
| `GET` | `/appointments/client/{clientId}` | Rendez-vous d’un client |

Appointment Service écoute sur le port `8083`.

### Exemples de requêtes Postman

Créer un rendez-vous avec la méthode `POST` et l’URL `http://localhost:8083/appointments`. Ajouter les en-têtes `Content-Type: application/json` et `Idempotency-Key: postman-demo-001`, puis ce corps (date future) :

```json
{
  "clientId": 1,
  "serviceId": 1,
  "appointmentDate": "2030-06-15T10:30:00",
  "status": "PENDING",
  "notes": "Première consultation"
}
```

Le statut reçu dans la requête est remplacé par `PENDING` à la création. Une nouvelle tentative avec la même clé retourne HTTP 400. Pour consulter ensuite, utiliser `GET http://localhost:8083/appointments/1` ou `GET http://localhost:8083/appointments/1/detail`.

Les réponses détaillées incluent les champs locaux du rendez-vous, `clientName`, `clientEmail`, `serviceName`, `serviceDescription`, `servicePrice` et `serviceDuration`. Ces informations distantes enrichissent uniquement la réponse.

### Scénarios d’erreur et vérification

| Cas | Réponse attendue |
|---|---|
| Création valide | `201 Created` |
| Clé absente, vide ou trop longue | `400 Bad Request` |
| Clé déjà utilisée | `400 Bad Request`, « Opération déjà traitée » |
| Client ou service distant introuvable | `404 Not Found` |
| Service présent mais indisponible | `400 Bad Request` |
| Échec de communication ou instance distante indisponible | `503 Service Unavailable` |
| Autre erreur HTTP Feign | `502 Bad Gateway` |
| Consultation simple/détaillée | `200 OK` si trouvé |
| Rendez-vous local introuvable | `404 Not Found` |

Exécuter les tests et l’installation Maven depuis la racine :

```powershell
mvn -pl appointment-service -am test
mvn clean install
```

Les tests unitaires simulant les pannes Feign vérifient la traduction des statuts sans arrêter les microservices.

## Actuator test URLs

Each application exposes only its configured Actuator endpoints over HTTP.

| Application | Port | Health | Info | Metrics |
|---|---:|---|---|---|
| Eureka Server | 8761 | <http://localhost:8761/actuator/health> | <http://localhost:8761/actuator/info> | <http://localhost:8761/actuator/metrics> |
| Client Service | 8081 | <http://localhost:8081/actuator/health> | <http://localhost:8081/actuator/info> | <http://localhost:8081/actuator/metrics> |
| Service Catalog | 8082 | <http://localhost:8082/actuator/health> | <http://localhost:8082/actuator/info> | <http://localhost:8082/actuator/metrics> |
| Appointment Service | 8083 | <http://localhost:8083/actuator/health> | <http://localhost:8083/actuator/info> | <http://localhost:8083/actuator/metrics> |
| API Gateway | 8090 | <http://localhost:8090/actuator/health> | <http://localhost:8090/actuator/info> | <http://localhost:8090/actuator/metrics> |

The Gateway also exposes read-only route inspection at:

- <http://localhost:8090/actuator/gateway/routes>

The Gateway endpoint can inspect routes but cannot create, refresh, or delete them. Environment and configuration endpoints are not exposed.
