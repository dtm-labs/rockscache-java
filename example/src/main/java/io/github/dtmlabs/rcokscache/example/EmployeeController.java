package io.github.dtmlabs.rcokscache.example;

import io.github.dtm.cache.Cache;
import io.github.dtm.cache.Consistency;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    private final Cache<Long, Employee> employeeCache;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            @Qualifier(CacheNames.EMPLOYEE) Cache<Long, Employee> employeeCache
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeCache = employeeCache;
    }

    @GetMapping("/{id}")
    public Employee find(
            @PathVariable("id") long id,
            @RequestParam(value = "consistency", defaultValue = "EVENTUAL") Consistency consistency
    ) {
        return employeeCache.fetch(id, consistency);
    }

    @PutMapping
    public void save(@RequestBody Employee employee) {

        employeeRepository.save(employee);

        /*
         * This example only shows the usage of rockscache-java. Therefore,
         * it directly calls the tagAsDeleted method after modifying the database
         * and does not consider that the tagAsDeleted method itself may throw exception.
         *
         * In actual projects, please ensure that tagAsDeleted will be executed successfully.
         * It is recommended to use reliable messages.
         *
         * For example, you can use the 2-phase message of the DTM framework to ensure that
         * tagAsDeleted will be retried and retried until it succeeds.
         *
         * Please see https://en.dtm.pub/practice/msg.html to know more
         */
        employeeCache.tagAsDeleted(employee.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") long id) {

        employeeRepository.deleteById(id);

        /*
         * This example only shows the usage of rockscache-java. Therefore,
         * it directly calls the tagAsDeleted method after modifying the database
         * and does not consider that the tagAsDeleted method itself may throw exception.
         *
         * In actual projects, please ensure that tagAsDeleted will be executed successfully.
         * It is recommended to use reliable messages.
         *
         * For example, you can use the 2-phase message of the DTM framework to ensure that
         * tagAsDeleted will be retried and retried until it succeeds.
         *
         * Please see https://en.dtm.pub/practice/msg.html to know more
         */
        employeeCache.tagAsDeleted(id);
    }
}
