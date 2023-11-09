package com.example.rqchallenge.controller;

import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.model.EmployeeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Controller
public class EmployeeExtController {

    private final Logger logger = LoggerFactory.getLogger(EmployeeExtController.class);
    private static final String CACHE_KEY_ALL_EMPLOYEES = "allEmployees";
    public static final String SUCCESS = "success";
    public static final String SALARY = "salary";

    private final RestTemplate restTemplate;

    private final String externalApiUrl;

    private final CacheManager cacheManager;

    @Autowired
    public EmployeeExtController(RestTemplate restTemplate, @Value("${external.api.url}") String externalApiUrl, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.externalApiUrl = externalApiUrl;
        this.cacheManager = cacheManager;
    }

    /**
     * Retrieves all employees from the external API and caches them.
     *
     * @return EmployeeResponse containing a list of all employees and status message
     */
    public EmployeeResponse getAllEmployeesFromApi() {
        return executeApiRequest(
                () -> restTemplate.getForEntity(externalApiUrl + "employees", EmployeeResponse.class),
                this::processEmployeeApiResponse
        );
    }

    /**
     * Fetches an employee by their ID from the external API.
     *
     * @param id The ID of the employee to be fetched
     * @return EmployeeResponse containing the employee details or an error message
     */
    public EmployeeResponse getEmployeeById(String id) {
        try {
            ResponseEntity<EmployeeResponse> response = restTemplate.getForEntity(externalApiUrl + "employee/" + id, EmployeeResponse.class);
            return processResponseById(id, response);
        } catch (Exception e) {
            return handleApiException(e, "Error while fetching employee by ID: " + id);
        }
    }

    private EmployeeResponse handleApiException(Exception e, String errorMessage) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            logger.error(errorMessage, httpException);
            return new EmployeeResponse(String.valueOf(httpException.getStatusCode().value()), Collections.emptyList(), httpException.getStatusText());
        } else {
            logger.error(errorMessage, e);
            return new EmployeeResponse(Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value()), Collections.emptyList(), "Internal server error");
        }
    }

    public ResponseEntity<Object> sendCreateEmployeeRequest(Map<String, Object> employeeInput) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("name", (String) employeeInput.get("name"));
        requestBody.add(SALARY, (String) employeeInput.get(SALARY));
        requestBody.add("age", (String) employeeInput.get("age"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
                externalApiUrl + "create",
                HttpMethod.POST,
                requestEntity,
                Object.class
        );
    }

    /**
     * Deletes an employee based on the provided ID.
     *
     * @param id The ID of the employee to delete
     * @return EmployeeResponse indicating the result of the deletion operation
     */
    public EmployeeResponse deleteEmployeeById(String id) {
        try {
            ResponseEntity<EmployeeResponse> response = restTemplate.exchange(
                    externalApiUrl + "delete/" + id,
                    HttpMethod.DELETE,
                    null,
                    EmployeeResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                EmployeeResponse responseBody = response.getBody();

                // Handle cases where 'data' might be null
                if (responseBody != null && responseBody.getData() == null) {
                    responseBody.setData(new ArrayList<>());
                }

                return responseBody;
            }
            return createErrorResponse(response.getStatusCode(), "Failed to delete the employee with ID: " + id);
        } catch (Exception e) {
            return handleApiException(e, "Error while deleting the employee with ID: " + id);
        }
    }

    private EmployeeResponse processResponseById(String id, ResponseEntity<EmployeeResponse> response) {
        if (response.getStatusCode() == HttpStatus.OK) {
            EmployeeResponse employeeResponse = response.getBody();
            if (employeeResponse != null && SUCCESS.equals(employeeResponse.getStatus()) && employeeResponse.getData().size() == 1) {
                return employeeResponse;
            }
            return new EmployeeResponse("500", Collections.emptyList(), "More than one employee found with ID");
        }
        return createErrorResponse(response.getStatusCode(), "Employee not found with ID: " + id);
    }

    private <T> EmployeeResponse executeApiRequest(Supplier<ResponseEntity<T>> requestSupplier, Function<T, EmployeeResponse> successHandler) {
        try {
            ResponseEntity<T> response = requestSupplier.get();
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return successHandler.apply(response.getBody());
            }
            return createErrorResponse(response.getStatusCode(), "Unexpected response format");
        } catch (Exception e) {
            return handleApiException(e, "API request failed");
        }
    }

    private EmployeeResponse createErrorResponse(HttpStatus status, String message) {
        return new EmployeeResponse(String.valueOf(status.value()), Collections.emptyList(), message);
    }

    private EmployeeResponse processEmployeeApiResponse(EmployeeResponse response) {
        if (SUCCESS.equals(response.getStatus())) {
            cacheData(CACHE_KEY_ALL_EMPLOYEES, response.getData());
            return response;
        }
        return new EmployeeResponse("500", Collections.emptyList(), "Invalid response status");
    }

    /**
     * Creates a new employee with the provided input data.
     *
     * @param employeeInput Map containing employee data such as name, salary, and age
     * @return EmployeeResponse indicating the result of the creation operation
     */
    public EmployeeResponse createEmployee(Map<String, Object> employeeInput) {
        try {
            ResponseEntity<Object> response = sendCreateEmployeeRequest(employeeInput);

            return processCreateEmployeeResponse(response);
        } catch (Exception e) {
            logger.error("Error while creating a new employee", e);
            return new EmployeeResponse("500", Collections.emptyList(), "Error while creating employee: " + e.getMessage());
        }
    }

    private EmployeeResponse processCreateEmployeeResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            try {
                // Convert the response body to a Map or a custom DTO
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseBody = mapper.convertValue(response.getBody(), new TypeReference<>() {});

                // Extract and process the data
                if (SUCCESS.equals(responseBody.get("status"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    if (data != null) {
                        Employee createdEmployee = convertMapToEmployee(data);
                        logger.info("Successfully created a new employee with ID: {}", createdEmployee.getId());
                        return new EmployeeResponse("201", Collections.singletonList(createdEmployee), "Employee created successfully");
                    }
                }
            } catch (Exception e) {
                logger.error("Error while processing response", e);
            }
        }
        return new EmployeeResponse("500", Collections.emptyList(), "Failed to create a new employee or process the response");
    }

    private void cacheData(String key, Object data) {
        Optional.ofNullable(cacheManager.getCache("employeeCache")).ifPresent(cache -> cache.put(key, data));
    }

    private Employee convertMapToEmployee(Map<String, Object> data) {
        Employee employee = new Employee();

        if (data.containsKey("name")) {
            employee.setEmployeeName((String) data.get("name"));
        }
        if (data.containsKey(SALARY)) {
            employee.setEmployeeSalary((String) data.get(SALARY));
        }
        if (data.containsKey("age")) {
            employee.setEmployeeAge(String.valueOf(data.get("age")));
        }
        if (data.containsKey("id")) {
            employee.setId(String.valueOf(data.get("id")));
        }

        employee.setProfileImage("");

        return employee;
    }
}
