package com.taskmanager.api.auth.service;
public interface RateLimitService { void check(String key, int limit); }
