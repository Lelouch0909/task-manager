package com.taskmanager.api.notification.repository;
import com.taskmanager.api.notification.model.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByOwnerId(UUID ownerId, Pageable pageable);
    Page<Notification> findByOwnerIdAndReadAtIsNull(UUID ownerId, Pageable pageable);
    long countByOwnerIdAndReadAtIsNull(UUID ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id and n.ownerId = :ownerId")
    Optional<Notification> findOwnedForUpdate(UUID ownerId, UUID id);
    @Modifying
    @Query("update Notification n set n.readAt = :now where n.ownerId = :ownerId and n.readAt is null")
    int markAllRead(UUID ownerId, Instant now);
}
