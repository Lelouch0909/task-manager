package com.taskmanager.api.auth.unit;
import com.taskmanager.api.auth.service.impl.SecretServiceImpl;
import com.taskmanager.api.auth.model.CodePurpose;
import com.taskmanager.api.common.config.AppProperties;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class SecretServiceTest {
    @Test void bindsCodesToAccountAndPurpose() {
        var service = new SecretServiceImpl(new AppProperties("x".repeat(32), "y".repeat(32), List.of(), false, "", "", ""));
        var user = UUID.randomUUID();
        var hash = service.codeHash(user, CodePurpose.VERIFY_EMAIL, "123456");
        assertThat(hash).hasSize(64).isNotEqualTo("123456");
        assertThat(service.codeHash(user, CodePurpose.RESET_PASSWORD, "123456")).isNotEqualTo(hash);
        assertThat(service.codeHash(UUID.randomUUID(), CodePurpose.VERIFY_EMAIL, "123456")).isNotEqualTo(hash);
        assertThat(service.newCode()).matches("[0-9]{6}");
        assertThat(service.newToken()).hasSize(43).isNotEqualTo(service.newToken());
    }
}
