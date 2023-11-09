package com.example.rqchallenge.controller;

import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.model.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeExtControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private EmployeeExtController employeeExtController;

    @Mock
    private Cache cache;

    private final String externalApiUrl = "http://example.com/api/";

    private List<Employee> mockEmployees = null;

    @BeforeEach
    public void setUp() {
        employeeExtController = new EmployeeExtController(restTemplate, externalApiUrl, cacheManager);
        mockEmployees = new ArrayList<>();
        mockEmployees.add(new Employee("1", "John Doe", "50000", "30", ""));
        mockEmployees.add(new Employee("2", "Jane Smith", "60000", "35", ""));
    }

    @Test
    public void testGetAllEmployeesFromApi_Success() {
        // Prepare mock response
        when(cacheManager.getCache("employeeCache")).thenReturn(cache);

        EmployeeResponse mockResponse = new EmployeeResponse("success", Collections.singletonList(mockEmployees.get(0)), "Fetched successfully");
        when(restTemplate.getForEntity(externalApiUrl + "employees", EmployeeResponse.class))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Call service method
        EmployeeResponse response = employeeExtController.getAllEmployeesFromApi();

        // Assertions
        assertEquals("success", response.getStatus());

        // Verify caching behavior
        verify(cache).put(anyString(), any());
    }

    @Test
    public void testGetEmployeeById_Success() {
        String id = "123";
        EmployeeResponse mockResponse = new EmployeeResponse("success", Collections.singletonList(mockEmployees.get(0)), "Employee found");
        when(restTemplate.getForEntity(externalApiUrl + "employee/" + id, EmployeeResponse.class))
                .thenReturn(ResponseEntity.ok(mockResponse));

        EmployeeResponse response = employeeExtController.getEmployeeById(id);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    public void testCreateEmployee_Failure() {
        Map<String, Object> employeeInput = new HashMap<>();
        employeeInput.put("name", "John Doe");
        employeeInput.put("salary", "test");
        employeeInput.put("age", "test");

        when(restTemplate.exchange(eq(externalApiUrl + "create"), eq(HttpMethod.POST), any(HttpEntity.class), eq(EmployeeResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));

        EmployeeResponse response = employeeExtController.createEmployee(employeeInput);

        assertEquals("500", response.getStatus());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    public void testDeleteEmployeeById_Success() {
        String id = "123";
        when(restTemplate.exchange(eq(externalApiUrl + "delete/" + id), eq(HttpMethod.DELETE), any(), eq(EmployeeResponse.class)))
                .thenReturn(ResponseEntity.ok().body(new EmployeeResponse("success", Collections.emptyList(), "Employee deleted")));

        EmployeeResponse response = employeeExtController.deleteEmployeeById(id);

        assertEquals("success", response.getStatus());
    }

    @Test
    void testCreateEmployee_ApiError() {
        Map<String, Object> employeeInput = new HashMap<>();
        employeeInput.put("name", "John Doe");
        employeeInput.put("salary", "50000");
        employeeInput.put("age", "30");

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(Object.class)))
                .thenThrow(new RuntimeException("API error"));

        EmployeeResponse result = employeeExtController.createEmployee(employeeInput);

        assertEquals("500", result.getStatus());
        assertTrue(result.getData().isEmpty());
        assertTrue(result.getMessage().contains("Error while creating employee"));
    }

    @Test
    public void testGetEmployeeById_NotFound() {
        String id = "nonExistingId";
        when(restTemplate.getForEntity(externalApiUrl + "employee/" + id, EmployeeResponse.class))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));

        EmployeeResponse response = employeeExtController.getEmployeeById(id);

        assertEquals("404", response.getStatus());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void testCreateEmployee_Success() {
        Map<String, Object> employeeInput = new HashMap<>();
        employeeInput.put("name", "John Doe");
        employeeInput.put("salary", "50000");
        employeeInput.put("age", "30");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("data", Map.of("name", "John Doe", "salary", "50000", "age", "30", "id", "123"));

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(Object.class)))
                .thenReturn(ResponseEntity.ok(responseMap));

        EmployeeResponse result = employeeExtController.createEmployee(employeeInput);

        assertEquals("201", result.getStatus());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("John Doe", result.getData().get(0).getEmployeeName());
    }

    @Test
    public void testDeleteEmployeeById_Failure() {
        String id = "nonExistingId";
        when(restTemplate.exchange(eq(externalApiUrl + "delete/" + id), eq(HttpMethod.DELETE), any(), eq(EmployeeResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));

        EmployeeResponse response = employeeExtController.deleteEmployeeById(id);

        assertEquals("404", response.getStatus());
    }


}
