# Football Booking — Backend

Spring Boot 3.2 REST API для системы бронирования спортивных площадок.

## Стек
- Java 17, Spring Boot 3.2
- Spring Security + JWT (Access + Refresh tokens)
- Hibernate / JPA
- Flyway migrations
- H2 (dev) / PostgreSQL (prod)
- Swagger UI

## Запуск (dev)
```bash
mvn spring-boot:run
```
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## Переменные окружения (prod)
```
JWT_SECRET=<base64-secret>
DB_URL=jdbc:postgresql://localhost:5432/footballdb
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Связанные репозитории
- [Android app](https://github.com/Muslim217/football-booking-android)
- [iOS app](https://github.com/Muslim217/football-booking-ios)
