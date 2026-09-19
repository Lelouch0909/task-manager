package com.taskmanager.api.architecture;
import com.taskmanager.api.ApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
class ModularArchitectureTest {
    @Test void domainsRespectTheirBoundaries() {
        ApplicationModules.of(ApiApplication.class).verify();
    }
}
