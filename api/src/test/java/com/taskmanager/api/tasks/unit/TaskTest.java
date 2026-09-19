package com.taskmanager.api.tasks.unit;
import com.taskmanager.api.tasks.model.*;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class TaskTest {
    @Test void normalizesTitleAndAllowsAllStatusTransitions() {
        var now = Instant.now();
        var task = new Task(UUID.randomUUID(), "  Bonjour  ", null, null, now);
        assertThat(task.getTitle()).isEqualTo("Bonjour");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        task.update("Modifié", "description", TaskStatus.DONE, now.plusSeconds(1));
        task.update("Modifié", null, TaskStatus.IN_PROGRESS, now.plusSeconds(2));
        assertThat(task.getCreatedAt()).isEqualTo(now);
        assertThat(task.getUpdatedAt()).isEqualTo(now.plusSeconds(2));
        assertThat(task.getDescription()).isNull();
    }
    @Test void rejectsInvalidTitleAndDescription() {
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), " ", null, null, Instant.now())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), "a".repeat(201), null, null, Instant.now())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), "ok", "a".repeat(5001), null, Instant.now())).isInstanceOf(ApiException.class);
    }
}
