package com.taskmanager.api.tasks.unit;
import com.taskmanager.api.tasks.service.impl.TaskServiceImpl;
import com.taskmanager.api.tasks.repository.TaskRepository;
import com.taskmanager.api.tasks.dto.request.UpdateTaskRequest;
import com.taskmanager.api.tasks.model.TaskStatus;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.Clock;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class TaskServiceTest {
    @Test void neverMutatesTasksOfAnotherOwner() {
        var repo = mock(TaskRepository.class);
        var owner = UUID.randomUUID(); var id = UUID.randomUUID();
        when(repo.findByIdAndOwnerId(id, owner)).thenReturn(Optional.empty());
        var service = new TaskServiceImpl(repo, Clock.systemUTC(), mock(org.springframework.context.ApplicationEventPublisher.class));
        assertThatThrownBy(() -> service.update(owner, id, new UpdateTaskRequest("Title", null, TaskStatus.DONE)))
            .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(404));
        assertThatThrownBy(() -> service.delete(owner, id)).isInstanceOf(ApiException.class);
        verify(repo, never()).delete(any(com.taskmanager.api.tasks.model.Task.class));
    }
    @Test void rejectsInvalidPaginationBeforeQueryingDatabase() {
        var repo = mock(TaskRepository.class);
        var service = new TaskServiceImpl(repo, Clock.systemUTC(), mock(org.springframework.context.ApplicationEventPublisher.class));
        assertThatThrownBy(() -> service.list(UUID.randomUUID(), -1, 20, null, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list(UUID.randomUUID(), 0, 101, null, null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(repo);
    }
}
