package com.bloodnetwork.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Blood Donor Network Spring Boot Web Application.
 * Scans all packages under 'com.bloodnetwork' including services, DAOs, and web controllers.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.bloodnetwork"})
public class BloodNetworkWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodNetworkWebApplication.class, args);
    }
}
