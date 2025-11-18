
# API Gateway (`API-GATEWAY`)

## Overview

The **API Gateway** is the single entry point for all client requests in the microservices ecosystem. Built on **Spring Cloud Gateway** (Reactive), it handles routing, security, rate limiting, and observability.

It acts as the "Front Door" of the system, ensuring that only authenticated requests reach the protected microservices while abstracting the internal network topology from the client.
## Features

* **Centralized Routing:** Routes external requests to the appropriate microservice (`USER-SERVICE`, `PRODUCT-SERVICE`, `ORDER-SERVICE`, `DELIVERY-SERVICE`) using Service Discovery.
* **JWT Authentication:** A custom `JwtAuthenticationFilter` intercepts requests, validates the Bearer token, and ensures the user is active before the request travels downstream.
* **Header Propagation:** Upon successful authentication, the Gateway extracts user details (`userId`, `email`, `roles`) and injects them into custom headers (`X-User-Id`, `X-User-Roles`) so downstream services don't need to re-parse the token.
* **Rate Limiting:** Implements a **Redis-based Token Bucket** algorithm to prevent abuse. Limits are applied based on User ID (if logged in) or IP Address.
* **Observability:** Integrated with Zipkin for distributed tracing and Prometheus for metrics.
## Tech Stack

| Layer | Technology Used |
| :--- | :--- |
| Core Framework | Spring Boot 3 (WebFlux) |
| Gateway Implementation | Spring Cloud Gateway |
| Security | Spring Security (Reactive), JJWT |
| Rate Limiting | Spring Cloud Gateway Redis Rate Limiter |
| Service Discovery | Spring Cloud Netflix Eureka Client |
| Configuration | Spring Cloud Config Client |
| Observability | Spring Boot Actuator (Prometheus, Zipkin) |
| Build Tool | Maven |
| Language | Java 21 |
| IDE | IntelliJ  |

---

## Prerequisites

Before running the Gateway, ensure the following infrastructure components are running:

1.  **Redis:** (Critical) Required for the Rate Limiter to store keys. Running on `localhost:6379`.
2.  **Eureka Discovery Server:** Running on `http://localhost:8761/eureka`.
3.  **Config Server:** Running on `http://localhost:8888`.
4.  **Zipkin:** Running on `http://localhost:9411`.
## Configuration

The Gateway pulls its configuration from the **Config Server**, but local overrides and secrets are defined in `application.yml`.

**`api-gateway/src/main/resources/application.yml`**

```yaml
server:
  port: 8085

spring:
  application:
    name: API-GATEWAY
  config:
    import: optional:configserver:http://localhost:8888
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        # See "Routing Logic" section below for details
        # ...

# --- !! REDIS FOR RATE LIMITING !! ---
  data:
    redis:
      host: localhost
      port: 6379
# -------------------------------------

# --- !! SECRETS (MUST MATCH USER-SERVICE) !! ---
jwt:
  secret: your-JWT-secret-key
  expiration: 600000
# ------------------------------------------

logging:
  level:
    com.microservices.api_gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      enabled: true
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```
## Running the Service

1.  Ensure **Redis** is running (`docker run -p 6379:6379 redis`).
2.  Navigate to the project root.
3.  Build the application:
    ```Bash
    mvn clean install
    ```
4.  Run the application:
    ```bash
    java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
    ```
5.  The Gateway will start on port **8085**.


## Security Model

The Gateway employs a sophisticated filter chain to secure the application.

### 1. `JwtAuthenticationFilter` (Custom Pre-Filter)
Applied to all protected routes (User, Product, Order, Delivery).
1.  Intercepts the request and extracts the `Authorization: Bearer <token>` header.
2.  Validates the token signature and expiration using `JwtUtil`.
3.  **Header Mutation:** If valid, it extracts claims and adds the following headers to the request before sending it downstream:
    * `X-User-Id`: The database ID of the user.
    * `X-User-Email`: The email of the user.
    * `X-User-Roles`: A comma-separated list of roles (e.g., `ROLE_USER,ROLE_ADMIN`).
4.  If invalid, returns `401 Unauthorized` immediately.

### 2. Rate Limiting (Redis)
Applied to **ALL** routes (including Auth).
* **Key Resolver:** `RateLimiterConfig` determines the key for the bucket.
    * If `X-User-Id` exists (authenticated), it limits based on **User ID**.
    * If not (public), it limits based on **Client IP Address**.
* **Settings:**
    * `replenishRate`: 10 requests per second.
    * `burstCapacity`: 20 requests (allows short spikes).
---
## Routing Logic

The Gateway maps external paths to internal microservices via Load Balancer (`lb://`).

| Route ID | Path Pattern | Target Service | Filters Applied |
| :--- | :--- | :--- | :--- |
| `auth-service` | `/api/v1/auth/**` | `USER-SERVICE` | RateLimiter |
| `user-service` | `/api/v1/users/**` | `USER-SERVICE` | **JwtAuth**, RateLimiter |
| `product-service` | `/api/v1/products/**` | `PRODUCT-SERVICE` | **JwtAuth**, RateLimiter |
| `order-service` | `/api/v1/orders/**` | `ORDER-SERVICE` | **JwtAuth**, RateLimiter |
| `delivery-service` | `/api/v1/delivery/**` | `DELIVERY-SERVICE` | **JwtAuth**, RateLimiter |

---

## Authors
**Utkarsh Gupta**
- utkarsh.gupta26095@gmail.com
- **Github** - [@utkarsh1369](https://www.github.com/utkarsh1369)

