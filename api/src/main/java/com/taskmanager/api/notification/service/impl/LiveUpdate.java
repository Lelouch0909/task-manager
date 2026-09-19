package com.taskmanager.api.notification.service.impl;
import java.util.UUID;
/** Internal invalidation signal; never used as the durable source of truth. */
public record LiveUpdate(UUID ownerId, boolean tasks, boolean notifications) {}
