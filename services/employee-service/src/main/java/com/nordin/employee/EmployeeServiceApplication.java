package com.nordin.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Employee Service — Microservicio de empleados.
 *
 * Responsabilidades:
 * - Gestión de empleados (GET, POST)
 * - No llama a ningún otro microservicio
 * - Es el servicio hoja del árbol de dependencias
 *
 * Flujo: API Gateway → employee-service → emp-db (PostgreSQL)
 */
@SpringBootApplication
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
