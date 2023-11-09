package com.example.rqchallenge.controller;

import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.model.EmployeeResponse;
import com.example.rqchallenge.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = "api/v1/employee")
public class EmployeeController implements IEmployeeController {

    private final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final EmployeeExtController employeeExtController;

    @Autowired
    public EmployeeController(EmployeeService employeeService, EmployeeExtController employeeExtController) {
        this.employeeService = employeeService;
        this.employeeExtController = employeeExtController;
    }

    @Cacheable("employeeCache")
    @Operation(summary = "Get a list of all employees")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all employees"),
            @ApiResponse(responseCode = "204", description = "No employees found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Employee>> getAllEmployees() {
        try {
            List<Employee> allEmployees = employeeService.getAllCachedEmployees();

            if (!allEmployees.isEmpty()) {
                logger.info("Successfully fetched employee data from cache.");
                return ResponseEntity.ok(allEmployees);
            } else {
                logger.warn("Cache returned an empty list of employees.");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching employees", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search for employees by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of matching employees"),
            @ApiResponse(responseCode = "404", description = "No employees found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString) {
        try {
            List<Employee> allEmployees = employeeService.getAllCachedEmployees();
            if(allEmployees.isEmpty()) {
                logger.warn("Cache returned an empty list of employees.");
                return ResponseEntity.notFound().build();
            }

            List<Employee> filteredEmployees = employeeService.getFilteredEmployees(searchString, allEmployees);
            if (filteredEmployees.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(filteredEmployees);
        } catch (Exception e) {
            logger.error("Error during employee search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Operation(summary = "Get an employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        EmployeeResponse response = employeeExtController.getEmployeeById(id);
        return ResponseEntity.status(Integer.parseInt(response.getStatus()))
                .body(response.getData() != null && !response.getData().isEmpty() ? response.getData().get(0) : null);
    }

    @Operation(summary = "Get the highest salary among all employees")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Highest salary found"),
            @ApiResponse(responseCode = "200", description = "No employees found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        try {
            List<Employee> allEmployees = employeeService.getAllCachedEmployees();
            if(allEmployees.isEmpty()) {
                logger.warn("Cache returned an empty list of employees.");
                return ResponseEntity.notFound().build();
            }
            Optional<Integer> highestSalaryOptional = employeeService.filterHighestSalary(allEmployees);

            EmployeeResponse response = highestSalaryOptional.map(salary -> new EmployeeResponse("200", null, salary.toString()))
                    .orElseGet(() -> new EmployeeResponse("404", Collections.emptyList(), "No employees found"));

            return ResponseEntity.ok(Integer.parseInt(response.getMessage()));
        } catch (Exception e) {
            logger.error("Error occurred while fetching the highest salary: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get the top ten highest earning employee names")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top ten names found"),
            @ApiResponse(responseCode = "404", description = "No employees found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        try {
            List<Employee> allEmployees = employeeService.getAllCachedEmployees();
            if(allEmployees.isEmpty()) {
                logger.warn("Cache returned an empty list of employees.");
                return ResponseEntity.notFound().build();
            }

            List<String> names = employeeService.getTopTenNames(allEmployees);

            if (names == null || names.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Arrays.asList(names.toString().split(",")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Create a new employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Employee> createEmployee(Map<String, Object> employeeInput) {
        EmployeeResponse response = employeeExtController.createEmployee(employeeInput);

        return ResponseEntity.status(Integer.parseInt(response.getStatus()))
                .body(response.getData() != null && !response.getData().isEmpty() ? response.getData().get(0) : null);
    }

    @Operation(summary = "Delete an employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        EmployeeResponse response = employeeExtController.deleteEmployeeById(id);
        return ResponseEntity.status(Integer.parseInt(response.getStatus())).body(response.getMessage());
    }

}
