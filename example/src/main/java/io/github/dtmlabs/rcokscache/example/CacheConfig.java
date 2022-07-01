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

    /*
     * This demo shows only one cache.
     *
     * In actual projects, you can create many caches
     * by many methods of this class with the annotation @Bean.
     */
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
