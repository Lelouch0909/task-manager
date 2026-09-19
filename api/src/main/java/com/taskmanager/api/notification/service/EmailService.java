package com.taskmanager.api.notification.service;

public interface EmailService {
    void sendVerification(String email, String code);
    void sendPasswordReset(String email, String code);
}
