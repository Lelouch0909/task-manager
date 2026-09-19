package com.taskmanager.api.tasks.repository;
import com.taskmanager.api.tasks.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);
}
