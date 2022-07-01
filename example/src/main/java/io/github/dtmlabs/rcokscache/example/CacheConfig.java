package io.github.dtmlabs.rcokscache.example;

import io.github.dtm.cache.Cache;
import io.github.dtm.cache.CacheClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    /*
     * Automatically created by rockscache-spring-boot,
     * you need configure spring-redis in application.yml
     */
    private final CacheClient cacheClient;

    private final EmployeeRepository employeeRepository;

    public CacheConfig(CacheClient cacheClient, EmployeeRepository employeeRepository) {
        this.cacheClient = cacheClient;
        this.employeeRepository = employeeRepository;
    }

    @Bean(CacheNames.EMPLOYEE)
    public Cache<Long, Employee> employeeCache() {
        return cacheClient
                .newCacheBuilder(
                        CacheNames.EMPLOYEE + "-",
                        Long.class,
                        Employee.class
                )
                .setJavaLoader(
                        Employee::getId,
                        employeeRepository::findAllById
                )
                .build();
    }
}
