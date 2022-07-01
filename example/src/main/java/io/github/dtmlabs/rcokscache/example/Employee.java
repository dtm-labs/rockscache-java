package io.github.dtmlabs.rcokscache.example;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author 陈涛
 */
/*
 * Must specify the annotation @Table when both
 * spring-data-jdbc and spring-data-redis is used,
 * otherwise, EmployeeRepository will not be created
 */
@Table("employee")
@Data
public class Employee {

    @Id
    private Long id;
    
    private String firstName;
    
    private String lastName;

    /*
     * `getId` must be declared explicitly,
     * otherwise, `Employee::getId` can not
     * be accepted by gradle build command
     */
    public Long getId() {
        return id;
    }
}
