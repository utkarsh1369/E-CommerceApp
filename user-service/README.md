# User Service (`USER-SERVICE`)

## Overview

The **User Service** is a core microservice responsible for managing all aspects of user identity, authentication, and authorization within the project. It handles user registration, login (issuing JWTs), profile management, and role-based access control (RBAC).

This service is the central authority for "who" a user is and "what" they are allowed to do.


## Features

* **User Registration:** Allows new users to register.
* **Secure Admin Creation:** Provides a one-time, secret-key-protected endpoint to create the initial `SUPER_ADMIN`.
* **JWT Authentication:** Generates a JSON Web Token (JWT) upon successful login.
* **Full User CRUD:** Provides endpoints for administrators to Create, Read, Update, and Delete user profiles.
* **Role Management:** Allows a `SUPER_ADMIN` to assign and modify user roles.
* **Robust Authorization:** Implements fine-grained access control using `@PreAuthorize` and Spring Security.
* **Dual-Path Security:** Trusts requests from the API Gateway (via headers) while also supporting direct token-based authentication.



## Tech Stack

| Layer | Technology Used |
|--------|------------------|
| Backend Framework | Spring Boot |
|Security | Spring Security(JWT Authentication,RBAC) |
| ORM | Spring Data JPA |
| Database |  PostgreSQL |
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

1.  **PostgreSQL Database:** A running instance accessible at `jdbc:postgresql://localhost:5432/userdb`.
2.  **Config Server:** Running on `http://localhost:8888`.
3.  **Eureka Discovery Server:** Running on `http://localhost:8761/eureka`.
4.  **API Gateway:** (Optional for direct testing, but required for full flow) Running on `http://localhost:8085`.
## Configuration


The service pulls its main configuration from the **Config Server**. However, you must provide a local `application.yml` for bootstrap properties, database credentials, and secrets.

**`user-service/src/main/resources/application.yml`**
```yaml
server:
  port: 8081

spring:
  application:
    name: USER-SERVICE
  config:
    import: optional:configserver:http://localhost:8888 # Connects to Config Server
  
  # --- !! MUST BE CONFIGURED LOCALLY !! ---
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: YOUR_POSTGRES_USERNAME
    password: YOUR_POSTGRES_PASSWORD
    driver-class-name: org.postgresql.Driver
  # ------------------------------------------

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# --- !! SECRETS (MUST MATCH API-GATEWAY) !! ---
jwt:
  secret: your-JWT-secret-key
  expiration: 600000 -> validity time of token
# ------------------------------------------

# --- !! SECRET FOR ADMIN CREATION !! ---
admin:
  secret:
    code: your-ADMIN-secret-code
# ------------------------------------------

# Observability & Logging
logging:
  file:
    name: logs/user-service.log
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
You must manually create the database in PostgreSQL before starting the service:

***SQL***

`CREATE DATABASE userdb;`

Spring Data JPA will automatically create the `users` and `user_roles` tables on startup.
## Running the Service

1. Ensure all prerequisites (PostgreSQL, Config Server, Eureka) are running.

2. Navigate to the project's root directory.

3. Build the application using Maven:

```Bash

`mvn clean install`
```
4. Run the application:
```bash
`java -jar target/user-service-0.0.1-SNAPSHOT.jar`
```
5. The service will start on port 8081 and register with Eureka.
## Security Model

This service has a robust, dual-filter security chain to handle requests from different sources.

**`GatewayAuthenticationFilter` (Priority 1):**

This filter runs first. It checks for specific headers (`X-User-Id`,` X-User-Email`,` X-User-Roles`) added by the **API Gateway**.

If these headers are present, it **trusts** them, builds a `UserPrincipal` with the provided roles, and populates the `SecurityContext`.

This is a "fast path" that bypasses JWT validation, as the gateway has already done it.

**`JwtAuthenticationFilter` (Priority 2):**

If the gateway headers are not found, this filter runs.

It looks for the standard `Authorization: Bearer <token>` header.

It validates the JWT, extracts the user's details from the `UserRepository`, and populates the `SecurityContext`.

This path is used for direct service-to-service calls or for testing without the gateway.

**Role-Based Access Control (RBAC)**
Access to endpoints is controlled by `@PreAuthorize` annotations using the following roles:

`SUPER_ADMIN`: Full access to all endpoints, including user deletion and role assignment.

`USER`: Standard user. Can log in and manage their own profile.

`PRODUCT_ADMIN`, `ORDER_ADMIN`, `DELIVERY_ADMIN`: Other roles that can be assigned by the `SUPER_ADMIN`.
## API Endpoints

The service exposes two main sets of endpoints. All protected endpoints expect a valid JWT.

**API Documentation**: `http://localhost:8081/swagger-ui/index.html`

**Authentication Endpoints (Public)**

#### Base Path ` api/v1/auth`

| Method | Endpoint        | Description            |
| ------ | --------------- | ---------------------- |
| POST   | `/register-user`  | Create new user with `USER` Role   |
| POST    | `/register-admin`| (Single-time setup) Creates the SUPER_ADMIN.|
| POST | `/login`         |Authenticates a user and returns a `token` and user details.         |


**User Management Endpoints (Protected)**

#### Base Path ` api/v1/users`

| Method | Endpoint | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | `SUPER_ADMIN` | Retrieves a list of all users. |
| `GET` | `/{userId}` | `SUPER_ADMIN` or Self | Retrieves the details for a specific user. |
| `GET` | `/email/{email}` | `SUPER_ADMIN` | Retrieves a user by their email address. |
| `PUT` | `/update/{userId}` | `SUPER_ADMIN` or Self | Updates a user's profile (name, phone, address). |
| `DELETE` | `/delete/{userId}` | `SUPER_ADMIN` | Deletes a user from the database. |
| `PATCH` | `/assign-role/{userId}` | `SUPER_ADMIN` | Assigns or updates the roles for a specific user. |

---

## Authors
**Utkarsh Gupta**
- utkarsh.gupta26095@gmail.com
- **Github** - [@utkarsh1369](https://www.github.com/utkarsh1369)

