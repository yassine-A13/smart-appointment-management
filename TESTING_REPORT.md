# Smart Appointment Management Architecture Test

## Results

| Validation | Expected | Observed |
|---|---|---|
| Eureka Server | Starts on 8761; health succeeds | Passed; HTTP 200, `UP` |
| Config Server | Serves Config Repository environments on 8888 | Passed; tested all four client applications with HTTP 200 |
| Config Client loading | Three services and Gateway start with only local name/import settings | Passed; each startup logged its Config Server environment |
| Eureka registration | Three business services and Gateway show `UP` | Passed; all four instances `UP` |
| Direct APIs | `GET /clients`, `/services`, `/appointments` return 200 | Passed |
| Dynamic Gateway routes | All three service-prefixed API URLs return 200 | Passed |
| Actuator | Eureka, business services, and Gateway health endpoints return `UP` | Passed; HTTP 200 |
| Gateway route inspection | Dynamic routes visible at `/actuator/gateway/routes` | Passed; four routes (three business services and Gateway itself) |

The four Config Client modules have no old local port, database, Eureka, or Gateway route settings: their local `application.properties` files contain only `spring.application.name` and the required non-optional `spring.config.import`.

## Startup commands

Run each command in a separate terminal from the project root, in this order.

The configured GitHub repository URL is still a placeholder. For local verification, Config Server used its native profile against the project `config-repo`:

```powershell
mvn -pl eureka-server spring-boot:run
mvn '-pl' 'config-server' 'spring-boot:run' '-B' '-Dspring-boot.run.profiles=native' '-Dspring-boot.run.arguments=--spring.cloud.config.server.native.search-locations=file:../config-repo'
mvn -pl client-service spring-boot:run
mvn -pl service-catalog spring-boot:run
mvn -pl appointment-service spring-boot:run
mvn '-pl' 'gateway-service' 'spring-boot:run' '-B' '-Dspring-boot.run.profiles=dynamique' '-Dspring-boot.run.arguments=--server.address=::1'
```

The Gateway command binds to IPv6 loopback because an unrelated Oracle listener occupies IPv4 port 8080 on this workstation. The configured application port remains 8080.

## Verification URLs

Config Server environments:

- <http://localhost:8888/client-service/default>
- <http://localhost:8888/service-catalog/default>
- <http://localhost:8888/appointment-service/default>
- <http://localhost:8888/gateway-service/dynamique>

Eureka registry: <http://localhost:8761/eureka/apps> (request JSON with `Accept: application/json`).

Direct APIs:

- <http://localhost:8081/clients>
- <http://localhost:8082/services>
- <http://localhost:8083/appointments>

Dynamic Gateway APIs:

- <http://[::1]:8080/client-service/clients>
- <http://[::1]:8080/service-catalog/services>
- <http://[::1]:8080/appointment-service/appointments>
- <http://[::1]:8080/actuator/gateway/routes>

Health endpoints return HTTP 200 with `{"status":"UP"}`:

- <http://localhost:8761/actuator/health>
- <http://localhost:8081/actuator/health>
- <http://localhost:8082/actuator/health>
- <http://localhost:8083/actuator/health>
- <http://[::1]:8080/actuator/health>

Config Server does not currently include Actuator, so its Config Repository environment URLs are used to verify its availability.
