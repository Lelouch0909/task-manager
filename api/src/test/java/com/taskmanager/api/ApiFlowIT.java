package com.taskmanager.api;

import com.taskmanager.api.support.*;
import com.taskmanager.api.auth.repository.*;
import com.taskmanager.api.auth.model.CodePurpose;
import com.taskmanager.api.auth.service.AuthService;
import com.taskmanager.api.notification.service.EmailService;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import tools.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.jwt-secret=integration-jwt-secret-at-least-32-bytes",
    "app.code-secret=integration-code-secret-at-least-32-bytes",
    "app.resend-api-key=test-resend-key", "app.resend-from=Task Manager <test@example.com>",
    "app.cookie-secure=false", "app.allowed-origins=http://localhost:5173",
    "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true",
    "management.endpoint.health.probes.enabled=true",
    "management.endpoint.health.group.readiness.include=readinessState,db",
    "management.endpoint.health.show-details=never"
})
@Import({TestcontainersConfiguration.class, ApiFlowIT.TimeConfig.class})
class ApiFlowIT {
    static final FakeResend resend = new FakeResend();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.resend-base-url", resend::url);
    }
    @TestConfiguration static class TimeConfig {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    @Autowired MutableClock clock;
    @Autowired UserAccountRepository users;
    @Autowired AuthCodeRepository codes;
    @Autowired AuthSessionRepository sessions;
    @Autowired AuthService auth;
    @Autowired EmailService emails;
    @LocalServerPort int port;
    final ObjectMapper mapper = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final String password = "Password1!";
    @BeforeEach void before() { clock.advance(Duration.ofHours(1)); resend.status.set(200); resend.delayMs.set(0); }
    @AfterAll static void stop() { resend.close(); }

    record Reply(int status, JsonNode json, HttpHeaders headers) {}
    record Account(String email, String token, String cookie) {}
    Reply call(String method, String path, Object body, String token, String cookie, String origin) {
        try {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(15));
            if (token != null) builder.header("Authorization", "Bearer " + token);
            if (cookie != null) builder.header("Cookie", cookie);
            if (origin != null) builder.header("Origin", origin);
            builder.header("Content-Type", "application/json");
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = response.body().isBlank() ? mapper.createObjectNode() :
                response.headers().firstValue("content-type").orElse("").contains("json") ? mapper.readTree(response.body()) : mapper.createObjectNode();
            return new Reply(response.statusCode(), json, response.headers());
        } catch (Exception ex) { throw new AssertionError(ex); }
    }
    Reply post(String path, Object body) { return call("POST", path, body, null, null, null); }
    String uniqueEmail() { return UUID.randomUUID() + "@example.com"; }
    void register(String email) {
        var response = post("/api/auth/register", Map.of("displayName", "Alice", "email", email, "password", password));
        assertThat(response.status()).as(response.json().toString()).isEqualTo(201);
        assertThat(response.json().has("passwordHash")).isFalse();
    }
    Account account() {
        String email = uniqueEmail(); register(email);
        assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", resend.code(email))).status()).isEqualTo(204);
        var login = post("/api/auth/login", Map.of("email", email, "password", password));
        assertThat(login.status()).as(login.json().toString()).isEqualTo(200);
        return new Account(email, login.json().get("accessToken").asText(), cookie(login));
    }
    String cookie(Reply response) {
        return response.headers().firstValue("set-cookie").orElseThrow().split(";")[0];
    }
    @Test void readinessIsPublicWithoutExposingDatabaseDetails() {
        var reply = call("GET", "/actuator/health/readiness", null, null, null, null);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("status").asText()).isEqualTo("UP");
        assertThat(reply.json().has("components")).isFalse();
        assertThat(call("GET", "/actuator/env", null, null, null, null).status()).isEqualTo(401);
    }

    @Test void completeCrudPaginationAndOwnerIsolation() {
        var alice = account(); var bob = account();
        var created = call("POST", "/api/tasks", Map.of("title", "  Préparer API  ", "description", "Recherche description"),
            alice.token(), null, null);
        assertThat(created.status()).as(created.json().toString()).isEqualTo(201);
        assertThat(created.json().get("title").asText()).isEqualTo("Préparer API");
        assertThat(created.json().get("status").asText()).isEqualTo("TODO");
        String id = created.json().get("id").asText();
        call("POST", "/api/tasks", Map.of("title", "Autre tâche", "status", "DONE"), alice.token(), null, null);
        var list = call("GET", "/api/tasks?size=1", null, alice.token(), null, null);
        assertThat(list.json().get("totalElements").asInt()).isEqualTo(2);
        assertThat(list.json().get("totalPages").asInt()).isEqualTo(2);
        assertThat(list.json().get("items").size()).isEqualTo(1);
        assertThat(call("GET", "/api/tasks?status=TODO&search=DESCRIPTION", null, alice.token(), null, null)
            .json().get("totalElements").asInt()).isEqualTo(1);
        assertThat(call("GET", "/api/tasks?search=%25", null, alice.token(), null, null)
            .json().get("totalElements").asInt()).isZero();
        assertThat(call("GET", "/api/tasks", null, bob.token(), null, null).json().get("items").size()).isZero();
        assertThat(call("PUT", "/api/tasks/" + id, Map.of("title", "Vol", "status", "DONE"), bob.token(), null, null).status()).isEqualTo(404);
        assertThat(call("DELETE", "/api/tasks/" + id, null, bob.token(), null, null).status()).isEqualTo(404);
        assertThat(call("PUT", "/api/tasks/" + id, Map.of("title", "Terminé", "status", "DONE"), alice.token(), null, null).status()).isEqualTo(200);
        assertThat(call("DELETE", "/api/tasks/" + id, null, alice.token(), null, null).status()).isEqualTo(204);
        assertThat(call("DELETE", "/api/tasks/" + id, null, alice.token(), null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/api/tasks?size=101", null, alice.token(), null, null).status()).isEqualTo(400);
        assertThat(call("GET", "/api/tasks?status=INVALID", null, alice.token(), null, null).status()).isEqualTo(400);
        assertThat(call("POST", "/api/tasks", Map.of("title", " "), alice.token(), null, null).status()).isEqualTo(400);
    }
    @Test void refreshRotatesAndReplayRevokesAccessImmediately() {
        var alice = account();
        assertThat(call("GET", "/api/auth/me", null, alice.token(), null, null).status()).isEqualTo(200);
        var refreshed = call("POST", "/api/auth/refresh", null, null, alice.cookie(), "http://localhost:5173");
        assertThat(refreshed.status()).as(refreshed.json().toString()).isEqualTo(200);
        assertThat(cookie(refreshed)).isNotEqualTo(alice.cookie());
        assertThat(refreshed.headers().firstValue("set-cookie").orElseThrow()).contains("HttpOnly", "SameSite=Lax", "Path=/api/auth");
        assertThat(call("POST", "/api/auth/refresh", null, null, alice.cookie(), null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/tasks", null, refreshed.json().get("accessToken").asText(), null, null).status()).isEqualTo(401);
        assertThat(call("POST", "/api/auth/refresh", null, null, cookie(refreshed), null).status()).isEqualTo(401);
    }
    @Test void logoutInvalidatesAccessAndClearsCookie() {
        var alice = account();
        var response = call("POST", "/api/auth/logout", null, null, alice.cookie(), "http://localhost:5173");
        assertThat(response.status()).isEqualTo(204);
        assertThat(response.headers().firstValue("set-cookie").orElseThrow()).contains("Max-Age=0");
        assertThat(call("GET", "/api/tasks", null, alice.token(), null, null).status()).isEqualTo(401);
        assertThat(post("/api/auth/logout", null).status()).isEqualTo(204);
    }
    @Test void resetRevokesAllSessionsAndDoesNotVerifyUnverifiedEmail() {
        var alice = account();
        var second = post("/api/auth/login", Map.of("email", alice.email(), "password", password));
        assertThat(post("/api/auth/password/forgot", Map.of("email", alice.email())).status()).isEqualTo(202);
        String code = resend.code(alice.email());
        assertThat(post("/api/auth/password/reset", Map.of("email", alice.email(), "code", code, "password", "NewPassword1!")).status()).isEqualTo(204);
        assertThat(call("GET", "/api/tasks", null, alice.token(), null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/tasks", null, second.json().get("accessToken").asText(), null, null).status()).isEqualTo(401);
        assertThat(post("/api/auth/login", Map.of("email", alice.email(), "password", password)).status()).isEqualTo(401);
        assertThat(post("/api/auth/login", Map.of("email", alice.email(), "password", "NewPassword1!")).status()).isEqualTo(200);
        assertThat(post("/api/auth/password/reset", Map.of("email", alice.email(), "code", code, "password", password)).status()).isEqualTo(400);

        String pending = uniqueEmail(); register(pending);
        post("/api/auth/password/forgot", Map.of("email", pending));
        assertThat(post("/api/auth/password/reset", Map.of("email", pending, "code", resend.code(pending), "password", "NewPassword1!")).status()).isEqualTo(204);
        assertThat(post("/api/auth/login", Map.of("email", pending, "password", "NewPassword1!")).status()).isEqualTo(403);
    }
    @Test void codeAttemptsPersistAndResendEnforcesCooldown() {
        String email = uniqueEmail(); register(email);
        String realCode = resend.code(email);
        assertThat(post("/api/auth/login", Map.of("email", email, "password", password)).status()).isEqualTo(403);
        assertThat(post("/api/auth/email/resend", Map.of("email", email)).status()).isEqualTo(429);
        String wrongCode = realCode.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++)
            assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", wrongCode)).status()).isEqualTo(400);
        assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", realCode)).status()).isEqualTo(400);
        var user = users.findByEmail(email).orElseThrow();
        assertThat(codes.findByUserIdAndPurpose(user.getId(), CodePurpose.VERIFY_EMAIL).orElseThrow().getAttempts()).isEqualTo(5);
        clock.advance(Duration.ofSeconds(61));
        assertThat(post("/api/auth/email/resend", Map.of("email", email)).status()).isEqualTo(202);
        assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", resend.code(email))).status()).isEqualTo(204);
        assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", resend.code(email))).status()).isEqualTo(400);
    }
    @Test void expiryOriginValidationAndMalformedRequests() {
        var alice = account();
        assertThat(call("POST", "/api/auth/refresh", null, null, alice.cookie(), "https://evil.example").status()).isEqualTo(403);
        assertThat(call("GET", "/api/tasks", null, null, null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/tasks", null, "invalid", null, null).status()).isEqualTo(401);
        assertThat(post("/api/auth/register", Map.of("displayName", "", "email", "bad", "password", "x")).status()).isEqualTo(400);
        clock.advance(Duration.ofMinutes(15));
        assertThat(call("GET", "/api/tasks", null, alice.token(), null, null).status()).isEqualTo(401);
        assertThat(call("POST", "/api/auth/refresh", null, null, alice.cookie(), null).status()).isEqualTo(200);
        clock.advance(Duration.ofDays(7));
        assertThat(call("POST", "/api/auth/refresh", null, null, alice.cookie(), null).status()).isEqualTo(401);
    }
    @Test void resendFailureKeepsAccountAndRecoveryWorks() {
        String email = uniqueEmail();
        resend.status.set(500);
        var response = post("/api/auth/register", Map.of("displayName", "Alice", "email", email, "password", password));
        assertThat(response.status()).as(response.json().toString()).isEqualTo(503);
        assertThat(response.json().get("code").asText()).isEqualTo("email_unavailable");
        assertThat(users.findByEmail(email)).isPresent();
        assertThat(response.json().toString()).doesNotContain("test-resend-key");
        resend.status.set(200);
        clock.advance(Duration.ofSeconds(61));
        assertThat(post("/api/auth/email/resend", Map.of("email", email)).status()).isEqualTo(202);
        assertThat(post("/api/auth/email/verify", Map.of("email", email, "code", resend.code(email))).status()).isEqualTo(204);
        assertThat(resend.lastAuthorization.get()).isEqualTo("Bearer test-resend-key");
        assertThat(post("/api/auth/password/forgot", Map.of("email", uniqueEmail())).status()).isEqualTo(202);
        assertThat(post("/api/auth/email/resend", Map.of("email", uniqueEmail())).status()).isEqualTo(202);
    }
    @Test void providerTimeoutIsBounded() {
        resend.delayMs.set(6000);
        try {
            assertThatThrownBy(() -> emails.sendVerification("timeout@example.com", "123456"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(503));
        } finally { resend.delayMs.set(0); }
    }
    @Test void concurrentRefreshAllowsOneWinnerThenRevokesSession() throws Exception {
        var alice = account();
        String token = alice.cookie().substring("refresh_token=".length());
        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            Callable<Boolean> work = () -> {
                gate.await();
                try { auth.refresh(token); return true; }
                catch (ApiException ex) { assertThat(ex.status()).isEqualTo(401); return false; }
            };
            var a = pool.submit(work); var b = pool.submit(work); gate.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(call("GET", "/api/tasks", null, alice.token(), null, null).status()).isEqualTo(401);
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentVerificationConsumesCodeOnce() throws Exception {
        String email = uniqueEmail(); register(email);
        String code = resend.code(email);
        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            Callable<Boolean> work = () -> {
                gate.await();
                try { auth.verify(new com.taskmanager.api.auth.dto.request.VerifyEmailRequest(email, code)); return true; }
                catch (ApiException ex) { assertThat(ex.status()).isEqualTo(400); return false; }
            };
            var a = pool.submit(work); var b = pool.submit(work); gate.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); }
    }
    @Test void notificationsPersistAndReadStateIsOwnerScoped() {
        var alice = account(); var bob = account();
        var task = call("POST", "/api/tasks", Map.of("title", "Notification"), alice.token(), null, null);
        String taskId = task.json().get("id").asText();
        var page = call("GET", "/api/notifications", null, alice.token(), null, null);
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.json().get("unreadCount").asInt()).isEqualTo(1);
        String id = page.json().get("items").get(0).get("id").asText();
        assertThat(call("PUT", "/api/notifications/" + id + "/read", Map.of("read", true), bob.token(), null, null).status()).isEqualTo(404);
        assertThat(call("PUT", "/api/notifications/" + id + "/read", Map.of("read", true), alice.token(), null, null).status()).isEqualTo(200);
        assertThat(call("GET", "/api/notifications?unreadOnly=true", null, alice.token(), null, null).json().get("items").size()).isZero();
        assertThat(call("PUT", "/api/notifications/" + id + "/read", Map.of("read", false), alice.token(), null, null).status()).isEqualTo(200);
        assertThat(call("PUT", "/api/tasks/" + taskId, Map.of("title", "Notification", "status", "DONE"), alice.token(), null, null).status()).isEqualTo(200);
        assertThat(call("DELETE", "/api/tasks/" + taskId, null, alice.token(), null, null).status()).isEqualTo(204);
        assertThat(call("GET", "/api/notifications", null, alice.token(), null, null).json().get("totalElements").asInt()).isEqualTo(3);
        assertThat(call("PUT", "/api/notifications/read-all", null, alice.token(), null, null).status()).isEqualTo(204);
        assertThat(call("GET", "/api/notifications/unread-count", null, alice.token(), null, null).json().get("unreadCount").asInt()).isZero();
        assertThat(call("GET", "/api/notifications", null, bob.token(), null, null).json().get("items").size()).isZero();
        assertThat(call("GET", "/api/notifications?size=101", null, alice.token(), null, null).status()).isEqualTo(400);
    }

    @Test void sseSignalsCommittedChangesAndClosesOnRevocation() throws Exception {
        var alice = account(); var bob = account();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events"))
            .header("Authorization", "Bearer " + alice.token()).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElseThrow()).contains("text/event-stream");
        var executor = Executors.newSingleThreadExecutor();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body()))) {
            java.util.concurrent.Callable<String> next = () -> {
                String line;
                while ((line = reader.readLine()) != null) if (line.startsWith("data:")) return line;
                return "closed";
            };
            assertThat(executor.submit(next).get(5, TimeUnit.SECONDS)).contains("\"tasks\":true");
            call("POST", "/api/tasks", Map.of("title", "Bob only"), bob.token(), null, null);
            call("POST", "/api/tasks", Map.of("title", "Alice live"), alice.token(), null, null);
            assertThat(executor.submit(next).get(5, TimeUnit.SECONDS)).contains("\"notifications\":true");
            var page = call("GET", "/api/notifications", null, alice.token(), null, null);
            var id = page.json().get("items").get(0).get("id").asText();
            call("PUT", "/api/notifications/" + id + "/read", Map.of("read", true), alice.token(), null, null);
            assertThat(executor.submit(next).get(5, TimeUnit.SECONDS)).contains("\"tasks\":false");
            call("POST", "/api/auth/logout", null, null, alice.cookie(), null);
            live.heartbeat();
            assertThat(executor.submit(next).get(5, TimeUnit.SECONDS)).isEqualTo("closed");
        } finally { response.body().close(); executor.shutdownNow(); }
    }
    @Autowired com.taskmanager.api.notification.service.impl.LiveUpdateServiceImpl live;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired com.taskmanager.api.tasks.service.TaskService taskService;

    @Test void rollbackDoesNotPersistNotification() {
        var alice = account();
        var owner = users.findByEmail(alice.email()).orElseThrow().getId();
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            taskService.create(owner, new com.taskmanager.api.tasks.dto.request.CreateTaskRequest("Rollback", null, null));
            status.setRollbackOnly();
        });
        assertThat(call("GET", "/api/notifications", null, alice.token(), null, null).json().get("totalElements").asInt()).isZero();
        assertThat(call("GET", "/api/tasks", null, alice.token(), null, null).json().get("totalElements").asInt()).isZero();
    }

    @Test void swaggerDescribesRoutesSecurityAndErrors() {
        var reply = call("GET", "/v3/api-docs", null, null, null, null);
        assertThat(reply.status()).as(reply.json().toString()).isEqualTo(200);
        var paths = reply.json().get("paths");
        for (String route : List.of("/api/auth/register", "/api/auth/login", "/api/auth/me", "/api/auth/refresh",
            "/api/auth/logout", "/api/auth/email/verify", "/api/auth/email/resend",
            "/api/auth/password/forgot", "/api/auth/password/reset", "/api/tasks", "/api/tasks/{id}",
            "/api/notifications", "/api/notifications/{id}/read", "/api/events"))
            assertThat(paths.has(route)).as(route).isTrue();
        assertThat(paths.get("/api/tasks").get("get").get("security").toString()).contains("bearerAuth");
        assertThat(paths.get("/api/tasks/{id}").get("put").get("responses").has("404")).isTrue();
        assertThat(paths.get("/api/auth/refresh").get("post").get("security").toString()).contains("refreshCookie");
        assertThat(reply.json().get("components").get("schemas").has("TaskPage")).isTrue();
        assertThat(call("GET", "/swagger-ui/index.html", null, null, null, null).status()).isEqualTo(200);
    }

    @Test void webFetchClientMatchesRealApiContracts() throws Exception {
        // Opt in: keep the backend integration suite independent of Node by default.
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("web.contract.tests"));
        var alice = account();
        var script = java.nio.file.Path.of("../web/tests/api-contract.mjs").toAbsolutePath();
        var process = new ProcessBuilder("node", script.toString()).redirectErrorStream(true);
        process.environment().put("API_TEST_BASE", "http://localhost:" + port + "/api");
        process.environment().put("API_TEST_EMAIL", alice.email());
        process.environment().put("API_TEST_PASSWORD", password);
        var child = process.start();
        if (!child.waitFor(30, TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("Web contract test timed out"); }
        String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(child.exitValue()).as(output).isZero();
        System.out.println(output);
    }
}
