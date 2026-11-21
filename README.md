# Smart Appointment Management

See [TESTING_REPORT.md](TESTING_REPORT.md) for infrastructure verification commands and expected results.

## Actuator test URLs

Each application exposes only its configured Actuator endpoints over HTTP.

| Application | Port | Health | Info | Metrics |
|---|---:|---|---|---|
| Eureka Server | 8761 | <http://localhost:8761/actuator/health> | <http://localhost:8761/actuator/info> | <http://localhost:8761/actuator/metrics> |
| Client Service | 8081 | <http://localhost:8081/actuator/health> | <http://localhost:8081/actuator/info> | <http://localhost:8081/actuator/metrics> |
| Service Catalog | 8082 | <http://localhost:8082/actuator/health> | <http://localhost:8082/actuator/info> | <http://localhost:8082/actuator/metrics> |
| Appointment Service | 8083 | <http://localhost:8083/actuator/health> | <http://localhost:8083/actuator/info> | <http://localhost:8083/actuator/metrics> |
| API Gateway | 8080 | <http://localhost:8080/actuator/health> | <http://localhost:8080/actuator/info> | <http://localhost:8080/actuator/metrics> |

The Gateway also exposes read-only route inspection at:

- <http://localhost:8080/actuator/gateway/routes>

The Gateway endpoint can inspect routes but cannot create, refresh, or delete them. Environment and configuration endpoints are not exposed.
