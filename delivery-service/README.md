
# Delivery Service (`DELIVERY-SERVICE`)

## Overview

The **Delivery Service** is the fulfillment engine of the microservices ecosystem. It manages the logistics of shipping orders, tracking delivery status, and assigning shipments to users.

It acts as the consumer of finalized orders and the producer of shipping updates, ensuring that customers are kept informed about the journey of their packages through real-time status tracking and event-driven notifications.
## Features

* **Delivery Creation:** scheduled deliveries for confirmed orders (requires interaction with **Order Service**).
* **State Machine Validation:** Enforces strict status transitions to ensure data integrity (e.g., `PENDING` -> `SHIPPED` -> `DELIVERED`).
* **Real-time Status Updates:** Allows `DELIVERY_ADMIN` to update tracking status.
* **Cross-Service Integration:**
    * Fetches Order details via **Feign Client**.
    * Fetches User details (email) via **Feign Client** to personalize notifications.
* **Event-Driven Architecture:** Publishes lifecycle events (`DeliveryCreated`, `DeliveryStatusChanged`) and Notification events to **Kafka**.
* **Role-Based Access:** Restricted access for creating and updating deliveries (`DELIVERY_ADMIN`), with view access for users (own deliveries) and admins.
## Tech Stack

| Layer | Technology Used |
| :--- | :--- |
| Backend Framework | Spring Boot |
| Security | Spring Security (JWT, Method Security) |
| ORM | Spring Data JPA |
| Database | MySQL |
| Inter-service Comm | Spring Cloud OpenFeign |
| Messaging/Events | Apache Kafka |
| Cashing         |  Redis  |
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

1.  **MySQL Database:** A running instance accessible at `jdbc:mysql://localhost:3306/deliverydb`.
2.  **Apache Kafka & Zookeeper:** Running on `localhost:9092`.
3.  **Config Server:** Running on `http://localhost:8888`.
4.  **Eureka Discovery Server:** Running on `http://localhost:8761/eureka`.
5.  **User Service:** (Required for fetching user emails) Running on `http://localhost:8081`.
6.  **Order Service:** (Required for validating orders) Running on `http://localhost:8083`.
## Configuration

The service pulls its main configuration from the **Config Server**. However, you must provide a local `application.yml` for bootstrap properties, database credentials, and secrets.

**`delivery-service/src/main/resources/application.yml`**

```yaml
server:
  port: 8084 # Distinct port

spring:
  application:
    name: DELIVERY-SERVICE
  config:
    import: optional:configserver:http://localhost:8888
  
  # --- !! MUST BE CONFIGURED LOCALLY !! ---
  datasource:
    url: jdbc:mysql://localhost:3306/deliverydb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
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
      delivery-events: delivery-events-topic
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
    name: logs/delivery-service.log
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

`CREATE DATABASE deliverydb;`

---

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
5. The service will start on port 8084 and register with Eureka.
## Security Model

This service utilizes a dual-filter security chain and Method-Level Security.

### Filters:
1.  **`GatewayAuthenticationFilter`:** Trusts `X-User-Id` and `X-User-Roles` headers from the Gateway.
2.  **`JwtAuthenticationFilter`:** Validates `Authorization: Bearer <token>` if Gateway headers are missing.

### Access Control & State Logic:
* `DELIVERY_ADMIN`: Exclusive rights to **Create** deliveries and **Update Status**.
* `SUPER_ADMIN`: Read-only global access.
* **Ownership Check**: Uses `@deliverySecurityService` to ensure regular users can only view their own delivery details.
* **State Validation**: The service enforces logic to prevent invalid updates (e.g., you cannot cancel a delivery once it has been `DELIVERED`).
---
## API Endpoints

**API Documentation**: `http://localhost:8084/swagger-ui/index.html`

#### Base Path `api/v1/delivery`

| Method | Endpoint | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/create` | `DELIVERY_ADMIN` | Create a new delivery for an existing Order. |
| `PATCH` | `/{deliveryId}/update-status` | `DELIVERY_ADMIN` | Update status (e.g., PENDING -> SHIPPED). Triggers Kafka events. |
| `GET` | `/` | `SUPER_ADMIN`, `DELIVERY_ADMIN` | Retrieve a list of ALL deliveries. |
| `GET` | `/{deliveryId}` | Admin or Owner | Retrieve details for a specific delivery. |
| `GET` | `/{deliveryId}/order` | Admin Roles | Retrieve the Order object associated with a delivery. |

---

## Authors
**Utkarsh Gupta**
- utkarsh.gupta26095@gmail.com
- **Github** - [@utkarsh1369](https://www.github.com/utkarsh1369)

