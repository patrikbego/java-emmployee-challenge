package com.example.rqchallenge.service;

import com.example.rqchallenge.controller.EmployeeExtController;
import com.example.rqchallenge.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private EmployeeExtController employeeExtController;

    @InjectMocks
    private EmployeeService employeeService;

    private List<Employee> mockEmployees = null;

    @BeforeEach
    public void setUp() {
        employeeService = new EmployeeService(employeeExtController, cacheManager);
        mockEmployees = new ArrayList<>();
        mockEmployees.add(new Employee("1", "John Doe", "50000", "30", ""));
        mockEmployees.add(new Employee("2", "Jane Smith", "60000", "35", ""));
    }

    @Test
    public void testGetFilteredEmployees_Found() {

        List<Employee> employeeList = employeeService.getFilteredEmployees("John", mockEmployees);

        assertNotNull(employeeList);
        assertEquals(employeeList.get(0).getEmployeeName(), "John Doe");
        assertFalse(employeeList.isEmpty());
    }

    @Test
    public void testGetFilteredEmployees_NotFound() {

        List<Employee> employeeList = employeeService.getFilteredEmployees("nonExistingString", mockEmployees);

        assertNotNull(employeeList);
        assertTrue(employeeList.isEmpty());
    }

    @Test
    public void testGetTopTenNames_Success() {
        List<String> employeeList = employeeService.getTopTenNames(mockEmployees);

        assertNotNull(employeeList);
        assertEquals(2, employeeList.size());
    }

    @Test
    public void testGetTopTenNames_NoEmployees() {
        List<String> employeeList = employeeService.getTopTenNames(Collections.emptyList());

        assertNotNull(employeeList);
        assertTrue(employeeList.isEmpty());
    }

    @Test
    public void testFilterHighestSalary_Success() {
        Optional<Integer> highestSalary = employeeService.filterHighestSalary(mockEmployees);

        assertNotNull(highestSalary);
        assertEquals(60000, highestSalary.get());
    }

    @Test
    public void testFilterHighestSalary_NoEmployees() {
        Optional<Integer> highestSalary = employeeService.filterHighestSalary(Collections.emptyList());

        assertNotNull(highestSalary);
        assertTrue(highestSalary.isEmpty());
    }

}
