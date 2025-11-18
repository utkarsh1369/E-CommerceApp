# Order Service (`ORDER-SERVICE`)

## Overview

The **Order Service** is the transactional heart of the microservices ecosystem. It handles the complete lifecycle of customer orders, maintains consistency across services via distributed logic, and communicates with external systems for fulfillment.

It serves as the bridge between the User and the fulfillment process, managing stock validation, delivery association, and asynchronous notifications.
## Features

* **Order Placement & Validation:** Validates stock availability with the **Product Service** before confirming orders.
* **Distributed Transaction Management (Saga Pattern):** Implements a manual compensation mechanism. If an order fails after stock reduction, it automatically triggers a "Rollback" (stock increase) to ensure data consistency.
* **Event-Driven Architecture:** Publishes `ORDER_CREATED` and `ORDER_UPDATED` events to **Kafka** for notification services.
* **External Service Integration:** Uses **Feign Clients** to communicate seamlessly with the Product and Delivery services.
* **Delivery Integration:** Exposes endpoints to fetch delivery details associated with specific orders via the **Delivery Service**.
* **Fine-Grained Security:** Implements custom security logic to ensure users can only modify their *own* orders.


## Tech Stack

| Layer | Technology Used |
| :--- | :--- |
| Backend Framework | Spring Boot |
| Security | Spring Security (JWT, Method Security) |
| ORM | Spring Data JPA |
| Database | MySQL |
| Inter-service Comm | Spring Cloud OpenFeign |
| Messaging/Events | Apache Kafka |
| Service Discovery | Spring Cloud Netflix Eureka Client |
| Configuration | Spring Cloud Config Client |
| API Documentation | Springdoc OpenAPI (Swagger) |
| Observability | Spring Boot Actuator (Prometheus, Zipkin) |
| Utilities | Lombok, Slf4j |
| Build Tool | Maven |
| Language | Java 21 |
| IDE | IntelliJ  |

---

## Prerequisites

Before running this service, ensure the following components are running:

1.  **MySQL Database:** A running instance accessible at `jdbc:mysql://localhost:3306/orderdb`.
2.  **Apache Kafka & Zookeeper:** Running on `localhost:9092` (for notifications).
3.  **Config Server:** Running on `http://localhost:8888`.
4.  **Eureka Discovery Server:** Running on `http://localhost:8761/eureka`.
5.  **Product Service:** (Required for stock validation) Running on `http://localhost:8082`.
6.  **Delivery Service:** (Required for delivery details) Running on `http://localhost:8084`.
## Configuration

The service pulls its main configuration from the **Config Server**. However, you must provide a local `application.yml` for bootstrap properties, database credentials, and secrets.

**`order-service/src/main/resources/application.yml`**

```yaml
server:
  port: 8083

spring:
  application:
    name: ORDER-SERVICE
  config:
    import: optional:configserver:http://localhost:8888
  
  # --- !! MUST BE CONFIGURED LOCALLY !! ---
  datasource:
    url: jdbc:mysql://localhost:3306/orderdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    username: YOUR_MYSQL_USERNAME
    password: YOUR_MYSQL_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # --- !! KAFKA CONFIGURATION !! ---
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    topic:
      notification: notification-topic
  # ------------------------------------------

  jpa:
    hibernate:
      ddl-auto: update 
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

# --- !! SECRETS (MUST MATCH AUTH SERVICE/GATEWAY) !! ---
jwt:
  secret: your-JWT-secret-key
# ------------------------------------------

# Observability
logging:
  file:
    name: logs/order-service.log
  level:
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```

## Database Setup

You must manually create the database in MySQL before starting the service:

***SQL***

`CREATE DATABASE orderdb;`


## Running the Service

1. Ensure all prerequisites (mySQL, Config Server, Eureka) are running.

2. Navigate to the project's root directory.

3. Build the application using Maven:

```Bash

`mvn clean install`
```
4. Run the application:
```bash
`java -jar target/user-service-0.0.1-SNAPSHOT.jar`
```
5. The service will start on port 8083 and register with Eureka.
## Security Model

This service utilizes a dual-filter security chain and Method-Level Security.

### Filters:
1.  **`GatewayAuthenticationFilter`:** Trusts `X-User-Id` and `X-User-Roles` headers from the Gateway.
2.  **`JwtAuthenticationFilter`:** Validates `Authorization: Bearer <token>` if Gateway headers are missing.

### Access Control:

* `SUPER_ADMIN` / `ORDER_ADMIN`: Full access.
* `USER`: Can create orders.
* **Ownership Check**: Uses `@orderSecurityService` to ensure users can only update or view their own orders.

---
## API Endpoints

**API Documentation**: `http://localhost:8083/swagger-ui/index.html`

#### Base Path `api/v1/orders`

| Method | Endpoint | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/` | `USER` | Create a new order (triggers stock reduction & Kafka event). |
| `PUT` | `/{orderId}` | Owner (User) | Update an existing order. |
| `GET` | `/` | `SUPER_ADMIN`, `ORDER_ADMIN` | Retrieve a list of ALL orders. |
| `GET` | `/{orderId}` | Admin or Owner | Retrieve details for a specific order. |
| `GET` | `/{orderId}/delivery` | Admin Roles | Retrieve delivery status/details (via Delivery Client). |

---

## Authors
**Utkarsh Gupta**
- utkarsh.gupta26095@gmail.com
- **Github** - [@utkarsh1369](https://www.github.com/utkarsh1369)

