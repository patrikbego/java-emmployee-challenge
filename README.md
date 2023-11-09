# Employee Management API

## Project Overview
This is a mock API project that uses REST to REST communication to manage employee data. 

## Tech Stack
This is a Gradle project built with Spring Boot. It uses Caffeine for caching and OpenAPI for API documentation.

## Project Structure
The project is structured into three main layers:
- Controller Layer: Handles HTTP requests and responses. Located in `src/main/java/com/example/rqchallenge/controller/`.
- Service Layer: Contains business logic. Located in `src/main/java/com/example/rqchallenge/service/`.
- Model Layer: Defines the data models. Located in `src/main/java/com/example/rqchallenge/model/`.

## How to Run
To run the project locally, follow these steps:
1. Clone the repository to your local machine.
2. Navigate to the project directory in your terminal.
3. Run the command `./gradlew bootRun`.

## Caching
The project uses Caffeine for caching. Caching is set up in the `EmployeeService` class, where the `@Cacheable` annotation is used to cache the result of the `getAllCachedEmployees` method.

## API Documentation
The project uses OpenAPI for API documentation. You can access the API documentation at `http://localhost:8080/swagger-ui.html` when the application is running.