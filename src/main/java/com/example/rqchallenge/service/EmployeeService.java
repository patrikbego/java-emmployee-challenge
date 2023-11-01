package com.example.rqchallenge.service;

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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private static final String CACHE_KEY_ALL_EMPLOYEES = "allEmployees";
    public static final String SUCCESS = "success";
    public static final String NO_EMPLOYEES_FOUND = "No employees found";
    public static final String SALARY = "salary";
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final String externalApiUrl;

    @Autowired
    public EmployeeService(RestTemplate restTemplate, CacheManager cacheManager, @Value("${external.api.url}") String externalApiUrl) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        this.externalApiUrl = externalApiUrl;
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

    private EmployeeResponse processEmployeeApiResponse(EmployeeResponse response) {
        if (SUCCESS.equals(response.getStatus())) {
            cacheData(CACHE_KEY_ALL_EMPLOYEES, response.getData());
            return response;
        }
        return new EmployeeResponse("500", Collections.emptyList(), "Invalid response status");
    }

    private void cacheData(String key, Object data) {
        Optional.ofNullable(cacheManager.getCache("employeeCache")).ifPresent(cache -> cache.put(key, data));
    }

    private <T> Optional<T> getFromCache(String key, Class<T> type) {
        return Optional.ofNullable(cacheManager.getCache("employeeCache")).map(cache -> cache.get(key, type));
    }

    /**
     * Retrieves all employees from the cache, or fetches them from the API if the cache is empty.
     *
     * @return List of all employees, either from the cache or API
     */
    public List<Employee> getAllCachedEmployees() {
        return getFromCache(CACHE_KEY_ALL_EMPLOYEES, List.class)
                .orElseGet(() -> {
                    EmployeeResponse response = getAllEmployeesFromApi();
                    return response.getData().isEmpty() ? Collections.emptyList() : response.getData();
                });
    }

    /**
     * Filters employees based on a given search string. The search is case-insensitive.
     *
     * @param searchString The string to filter employees by
     * @param allEmployees The list of all employees to search within
     * @return EmployeeResponse containing the filtered list of employees and status message
     */
    public EmployeeResponse getFilteredEmployees(String searchString, List<Employee> allEmployees) {
        if (allEmployees.isEmpty()) {
            return new EmployeeResponse("404", Collections.emptyList(), NO_EMPLOYEES_FOUND);
        }
        String lowerCaseSearchString = searchString.toLowerCase();
        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(employee -> employee.getEmployeeName().toLowerCase().contains(lowerCaseSearchString))
                .collect(Collectors.toList());

        return filteredEmployees.isEmpty()
                ? new EmployeeResponse("404", Collections.emptyList(), NO_EMPLOYEES_FOUND)
                : new EmployeeResponse("200", filteredEmployees, "Employees found");
    }

    /**
     * Retrieves the top ten highest earning employees.
     *
     * @param allEmployees The list of all employees to search within
     * @return EmployeeResponse containing the names of the top ten highest earners
     */
    public EmployeeResponse getTopTenNames(List<Employee> allEmployees) {
        if (allEmployees.isEmpty()) {
            return new EmployeeResponse("404", Collections.emptyList(), NO_EMPLOYEES_FOUND);
        }

        List<String> topTenNames = allEmployees.stream()
                .sorted(Comparator.comparingInt(employee -> -Integer.parseInt(employee.getEmployeeSalary())))
                .limit(10)
                .map(Employee::getEmployeeName)
                .collect(Collectors.toList());

        return topTenNames.isEmpty()
                ? new EmployeeResponse("404", Collections.emptyList(), NO_EMPLOYEES_FOUND)
                : new EmployeeResponse("200", null, topTenNames.toString());
    }

    /**
     * Determines the highest salary among all employees.
     *
     * @param allEmployees The list of all employees
     * @return EmployeeResponse containing the highest salary found
     */
    public EmployeeResponse filterHighestSalary(List<Employee> allEmployees) {
        Optional<Integer> highestSalary = allEmployees.stream()
                .map(employee -> Integer.parseInt(employee.getEmployeeSalary()))
                .max(Integer::compare);

        return highestSalary.map(salary -> new EmployeeResponse("200", null, salary.toString()))
                .orElseGet(() -> new EmployeeResponse("404", Collections.emptyList(), NO_EMPLOYEES_FOUND));
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

    private ResponseEntity<Object> sendCreateEmployeeRequest(Map<String, Object> employeeInput) {
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

    private EmployeeResponse createErrorResponse(HttpStatus status, String message) {
        return new EmployeeResponse(String.valueOf(status.value()), Collections.emptyList(), message);
    }

}
