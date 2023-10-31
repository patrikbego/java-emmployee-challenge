package com.example.rqchallenge.model;

import java.util.Collections;
import java.util.List;

/**
 * A response model for employee-related operations.
 */
public class EmployeeResponse {
    private List<Employee> data;
    private String status;
    private String message;

    public EmployeeResponse(String status, List<Employee> data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public EmployeeResponse(String status, String message) {
        this(status, Collections.emptyList(), message);
    }

    public EmployeeResponse() {
    }

    public List<Employee> getData() {
        return data;
    }

    public void setData(List<Employee> data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "EmployeeResponse{" +
                "data=" + data +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
