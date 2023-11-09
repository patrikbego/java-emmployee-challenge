package com.example.rqchallenge;

import com.example.rqchallenge.controller.EmployeeExtController;
import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.model.EmployeeResponse;
import com.example.rqchallenge.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RqChallengeApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private EmployeeExtController employeeExtController;

    private List<Employee> employees;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        Employee employee1 = new Employee("1", "John Doe", "50000", "30", "1");
        Employee employee2 = new Employee("2", "Jane Smith", "60000", "35", "2");
        employees = Arrays.asList(employee1, employee2);
    }

    @Test
    @DirtiesContext
    public void getAllEmployeesTest() throws Exception {
        given(employeeService.getAllCachedEmployees()).willReturn(employees);

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(employees.size()));
    }

    @Test
    @DirtiesContext
    public void getAllEmployees_ServerError() throws Exception {
        // Simulate an exception thrown by the service
        given(employeeService.getAllCachedEmployees()).willThrow(new RuntimeException("Internal error"));

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isInternalServerError());
    }


    @Test
    public void getAllEmployeesWhenNoneExistTest() throws Exception {
        // Simulate an empty list returned by the service
        given(employeeService.getAllCachedEmployees()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getEmployeesByNameSearch_Success() throws Exception {
        String searchString = "John";
        List<Employee> mockEmployees = Arrays.asList(
                new Employee("1", "John Doe", "60000", "30", "1"),
                new Employee("2", "Johnny Bravo", "70000", "35", "2")
        );

        given(employeeService.getAllCachedEmployees()).willReturn(mockEmployees);
        given(employeeService.getFilteredEmployees(eq(searchString), anyList()))
                .willReturn(mockEmployees);

        mockMvc.perform(get("/api/v1/employee/search/" + searchString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employee_name", is("John Doe")))
                .andExpect(jsonPath("$[1].employee_name", is("Johnny Bravo")));
    }

    @Test
    public void getEmployeesByNameSearch_NotFound() throws Exception {
        String searchString = "Unknown";
        List<Employee> emptyList = Collections.emptyList();

        given(employeeService.getAllCachedEmployees()).willReturn(emptyList);
        given(employeeService.getFilteredEmployees(eq(searchString), anyList()))
                .willReturn(emptyList);

        mockMvc.perform(get("/api/v1/employee/search/" + searchString))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getEmployeesByNameSearch_ServerError() throws Exception {
        String searchString = "test";
        given(employeeService.getAllCachedEmployees()).willThrow(new RuntimeException("Internal error"));

        mockMvc.perform(get("/api/v1/employee/search/" + searchString))
                .andExpect(status().isInternalServerError());
    }



    @Test
    public void getEmployeeByIdTest() throws Exception {
        String employeeId = "123";
        Employee employee = new Employee("123", "John Doe", "60000", "30", "1");

        given(employeeExtController.getEmployeeById(employeeId)).willReturn(new EmployeeResponse("200", List.of(employee), null));

        mockMvc.perform(get("/api/v1/employee/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(employeeId));
    }

    @Test
    public void getEmployeeById_NotFound() throws Exception {
        String employeeId = "unknown_id";
        EmployeeResponse mockResponse = new EmployeeResponse("404", Collections.emptyList(), null);

        given(employeeExtController.getEmployeeById(employeeId)).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/employee/" + employeeId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getEmployeeById_ServerError() throws Exception {
        String employeeId = "3";
        given(employeeExtController.getEmployeeById(employeeId)).willReturn(new EmployeeResponse("500", Collections.emptyList(), null));

        mockMvc.perform(get("/api/v1/employee/" + employeeId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getHighestSalary_Success() throws Exception {
        // Mock the service call
        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setStatus("200");
        mockResponse.setMessage("80000");

        given(employeeService.getAllCachedEmployees()).willReturn(employees);
        given(employeeService.filterHighestSalary(employees)).willReturn(Optional.of(80000));

        // Perform the request and assert the results
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("80000"));
    }

    @Test
    public void getHighestSalary_NoEmployeesFound() throws Exception {
        // Mock the service call with an empty response
        given(employeeService.getAllCachedEmployees()).willReturn(Collections.emptyList());

        // Perform the request and assert the results
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getHighestSalary_InternalServerError() throws Exception {
        // Force an exception when the service method is called
        given(employeeService.getAllCachedEmployees()).willThrow(new RuntimeException("Internal server error"));

        // Perform the request and assert the results
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getTopTenHighestEarningEmployeeNames_Success() throws Exception {
        // Mock setup
        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setStatus("200");
        mockResponse.setMessage("Alice,Bob,Charlie");

        given(employeeService.getAllCachedEmployees()).willReturn(employees);
        given(employeeService.getTopTenNames(employees)).willReturn(Arrays.asList("Alice,Bob,Charlie".split(",")));

        // Perform request and assert results
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    public void getTopTenHighestEarningEmployeeNames_NoEmployeesFound() throws Exception {
        // Mock the service call with an empty response
        given(employeeService.getAllCachedEmployees()).willReturn(Collections.emptyList());

        // Perform request and assert results
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getTopTenHighestEarningEmployeeNames_InternalServerError() throws Exception {
        // Force an exception when the service method is called
        given(employeeService.getAllCachedEmployees()).willThrow(new RuntimeException("Internal server error"));

        // Perform request and assert results
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isInternalServerError());
    }


    @Test
    public void createEmployee_Success() throws Exception {
        // Mock data
        Map<String, Object> employeeInput = new HashMap<>();
        employeeInput.put("name", "John Doe");
        employeeInput.put("salary", "60000");
        employeeInput.put("age", "30");

        Employee newEmployee = new Employee("1", "John Doe", "60000", "30", "1");

        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setStatus("201");
        mockResponse.setData(Collections.singletonList(newEmployee));

        // Mock the service call
        given(employeeExtController.createEmployee(employeeInput)).willReturn(mockResponse);

        // Convert employeeInput to JSON
        String employeeJson = objectMapper.writeValueAsString(employeeInput);

        // Perform request and assert results
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee_name").value(newEmployee.getEmployeeName()))
                .andExpect(jsonPath("$.employee_salary").value(newEmployee.getEmployeeSalary()))
                .andExpect(jsonPath("$.employee_age").value(newEmployee.getEmployeeAge()));
    }

    @Test
    public void createEmployee_InternalServerError() throws Exception {
        // Mock data
        Map<String, Object> employeeInput = new HashMap<>();
        employeeInput.put("name", "John Doe");
        employeeInput.put("salary", "60000");
        employeeInput.put("age", "30");

        // Force an exception when the service method is called
        EmployeeResponse mockResponse = new EmployeeResponse("500", Collections.emptyList(), null);
        given(employeeExtController.createEmployee(employeeInput)).willReturn(mockResponse);

        // Convert employeeInput to JSON
        String employeeJson = objectMapper.writeValueAsString(employeeInput);

        // Perform request and assert results
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void deleteEmployeeById_Success() throws Exception {
        String employeeId = "1";
        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setStatus("200");
        mockResponse.setMessage("Employee deleted successfully");

        given(employeeExtController.deleteEmployeeById(employeeId)).willReturn(mockResponse);

        mockMvc.perform(delete("/api/v1/employee/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(content().string("Employee deleted successfully"));
    }

    @Test
    public void deleteEmployeeById_NotFound() throws Exception {
        String employeeId = "999"; // Assuming this ID does not exist
        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setStatus("404");
        mockResponse.setMessage("Employee not found");

        given(employeeExtController.deleteEmployeeById(employeeId)).willReturn(mockResponse);

        mockMvc.perform(delete("/api/v1/employee/" + employeeId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Employee not found"));
    }

    @Test
    public void deleteEmployeeById_InternalServerError() throws Exception {
        String employeeId = "1";

        given(employeeExtController.deleteEmployeeById(employeeId)).willReturn(new EmployeeResponse("500", Collections.emptyList(), null));

        mockMvc.perform(delete("/api/v1/employee/" + employeeId))
                .andExpect(status().isInternalServerError());
    }

}
