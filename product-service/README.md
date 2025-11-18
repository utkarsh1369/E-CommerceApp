# Product Service (`PRODUCT-SERVICE`)

## Overview

The **Product Service** is a core microservice responsible for managing the catalog of items available within the application. It handles the creation, updates, retrieval, and deletion of products.

This service acts as the single source of truth for product details, pricing, and inventory availability.


## Features

* **Product Catalog Management:** Create, update, and manage product details (name, description, price, inventory).
* **Inventory Tracking:** Manage stock levels and availability status.
* **Public Browsing:** Allows users (and guests, if configured) to view products and details without administrative privileges.
* **Role-Based Administration:** Only users with specific admin roles can modify the catalog.
* **Dual-Path Security:** Trusts requests from the API Gateway (via headers) while also supporting direct token-based authentication.
* **Robust Validation:** Ensures product data integrity (non-negative prices, valid stock counts) before persistence.



## Tech Stack

| Layer | Technology Used |
|--------|------------------|
| Backend Framework | Spring Boot |
|Security | Spring Security(JWT Authentication,RBAC) |
| ORM | Spring Data JPA |
| Database |  mySQL |
| Service Discovery | Spring Cloud Netflix Eureka Client |
| Configuration | Spring Cloud Config Client | 
| API Documentation | Springdoc OpenAPI(Swagger) |
| Observability | Spring Boot Actuator(Prometheus,Zipkin) |
| Utilities | Lombok,Slf4j | 
| Build Tool | Maven |
| Language | Java 21 |
| IDE | IntelliJ  |

---

## Prerequisites

Before running this service, ensure the following components are running:

1.  **MySQL Database:** A running instance accessible at `jdbc:mysql://localhost:3306/productdb`.
2.  **Config Server:** Running on `http://localhost:8888`.
3.  **Eureka Discovery Server:** Running on `http://localhost:8761/eureka`.
4.  **API Gateway:** (Optional for direct testing, but required for full flow) Running on `http://localhost:8085`.
## Configuration


The service pulls its main configuration from the **Config Server**. However, you must provide a local `application.yml` for bootstrap properties, database credentials, and secrets.

**`product-service/src/main/resources/application.yml`**
```yaml
server:
  port: 8082 # Distinct port from User Service

spring:
  application:
    name: PRODUCT-SERVICE
  config:
    import: optional:configserver:http://localhost:8888 # Connects to Config Server
  
  # --- !! MUST BE CONFIGURED LOCALLY !! ---
  datasource:
    url: jdbc:mysql://localhost:3306/productdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    username: YOUR_MYSQL_USERNAME
    password: YOUR_MYSQL_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  # ------------------------------------------

  jpa:
    hibernate:
      ddl-auto: update # Change to validate in production
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

# --- !! SECRETS (MUST MATCH AUTH SERVICE/GATEWAY) !! ---
jwt:
  secret: your-JWT-secret-key # Used to validate tokens for direct access
# ------------------------------------------

# Observability & Logging
logging:
  file:
    name: logs/product-service.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  level:
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans # Zipkin URL
  tracing:
    sampling:
      probability: 1.0
```

## Database Setup
You must manually create the database in mySQL before starting the service:

***SQL***

`CREATE DATABASE productdb;`

Spring Data JPA will automatically create the `products` (and related) tables on startup.
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
5. The service will start on port 8082 and register with Eureka.
## Security Model

This service has a robust, dual-filter security chain to handle requests from different sources.

**`GatewayAuthenticationFilter` (Priority 1):**

This filter runs first. It checks for specific headers (`X-User-Id`,` X-User-Roles`) added by the **API Gateway**.

If these headers are present, it **trusts** them, builds a `UserPrincipal` with the provided roles(eg. `PRODUCT_ADMIN`).


**`JwtAuthenticationFilter` (Priority 2):**

If the gateway headers are not found, this filter runs.

It looks for the standard `Authorization: Bearer <token>` header.

This path is used for direct service-to-service calls or for testing without the gateway.

**Role-Based Access Control (RBAC)**
Access to endpoints is controlled by `@PreAuthorize` annotations using the following roles:

`PRODUCT_ADMIN`: Has full access to create,update, and delete products.

`SUPER_ADMIN`: inherits all permissions.

`USER`/`Anonymous`: Can typically view products(`GET` requests) but cannot update data.

## API Endpoints

**API Documentation**: `http://localhost:8082/swagger-ui/index.html`


**Product Management Endpoints (Protected)**

#### Base Path ` api/v1/products`

| Method | Endpoint | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | `PRODUCT_ADMIN`,`SUPER_ADMIN`,`USER`,`ORDER_ADMIN` | Retrieves a paginated list of all products. |
| `GET` | `/{productId}` | `PRODUCT_ADMIN`,`SUPER_ADMIN`,`USER`,`ORDER_ADMIN` | Retrieves details for a specific product. |
| `POST` | `/add` | `PRODUCT_ADMIN`,`SUPER_ADMIN` | Creates a new product in the catalog. |
| `PUT` | `/update/{productId}` | `PRODUCT_ADMIN`,`SUPER_ADMIN` | Updates product details (price, description, attributes). |
| `DELETE` | `/delete/{productId}` | `PRODUCT_ADMIN`,`SUPER_ADMIN` | Removes a product from the database. |
| `PATCH` | `/reduce-stock/{productId}` | `PRODUCT_ADMIN`,`SUPER_ADMIN`,`USER` | Updates the stock/inventory count for a product. |
| `PATCH` | `/increase-stock/{productId}` | `PRODUCT_ADMIN`,`SUPER_ADMIN`,`USER` | Updates the stock/inventory count for a product. |

---

## Authors
**Utkarsh Gupta**
- utkarsh.gupta26095@gmail.com
- **Github** - [@utkarsh1369](https://www.github.com/utkarsh1369)

