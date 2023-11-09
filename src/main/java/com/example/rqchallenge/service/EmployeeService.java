package com.example.rqchallenge.service;

import com.example.rqchallenge.controller.EmployeeExtController;
import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.model.EmployeeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private static final String CACHE_KEY_ALL_EMPLOYEES = "allEmployees";
    private final EmployeeExtController employeeExtController;
    private final CacheManager cacheManager;

    @Autowired
    public EmployeeService(EmployeeExtController employeeExtController, CacheManager cacheManager) {
        this.employeeExtController = employeeExtController;
        this.cacheManager = cacheManager;
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
                    EmployeeResponse response = employeeExtController.getAllEmployeesFromApi();
                    return response.getData().isEmpty() ? Collections.emptyList() : response.getData();
                });
    }

    /**
     * Filters employees based on a given search string. The search is case-insensitive.
     *
     * @param searchString The string to filter employees by
     * @param allEmployees The list of all employees to search within
     * @return List of filtered employees
     */
    public List<Employee> getFilteredEmployees(String searchString, List<Employee> allEmployees) {
        String lowerCaseSearchString = searchString.toLowerCase();
        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(employee -> employee.getEmployeeName().toLowerCase().contains(lowerCaseSearchString))
                .collect(Collectors.toList());
        return filteredEmployees;
    }

    /**
     * Retrieves the top ten highest earning employees.
     *
     * @param allEmployees The list of all employees to search within
     * @return EmployeeResponse containing the names of the top ten highest earners
     */
    public List<String> getTopTenNames(List<Employee> allEmployees) {

        List<String> topTenNames = allEmployees.stream()
                .sorted(Comparator.comparingInt(employee -> -Integer.parseInt(employee.getEmployeeSalary())))
                .limit(10)
                .map(Employee::getEmployeeName)
                .collect(Collectors.toList());

        return topTenNames;
    }

    /**
     * Determines the highest salary among all employees.
     *
     * @param allEmployees The list of all employees
     * @return Optional containing the highest salary found
     */
    public Optional<Integer> filterHighestSalary(List<Employee> allEmployees) {
        Optional<Integer> highestSalary = allEmployees.stream()
                .map(employee -> Integer.parseInt(employee.getEmployeeSalary()))
                .max(Integer::compare);
        return highestSalary;
    }

}
