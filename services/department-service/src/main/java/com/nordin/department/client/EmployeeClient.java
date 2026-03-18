package com.nordin.department.client;

import com.nordin.department.dto.EmployeeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "employee-service",
        url = "${EMPLOYEE_SERVICE_URL:http://localhost:8083}",
        fallback = EmployeeClientFallback.class
)
public interface EmployeeClient {

    @GetMapping("/api/employees/department/{departmentId}")
    List<EmployeeResponse> getEmployeesByDepartmentId(@PathVariable UUID departmentId);
}
