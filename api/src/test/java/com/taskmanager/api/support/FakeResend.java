package com.taskmanager.api.support;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class FakeResend implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, String> codes = new ConcurrentHashMap<>();
    public final AtomicInteger status = new AtomicInteger(200);
    public final AtomicLong delayMs = new AtomicLong(0);
    public final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    public FakeResend() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/emails", exchange -> {
                try {
                    lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    var json = mapper.readTree(exchange.getRequestBody().readAllBytes());
                    var matcher = Pattern.compile(">([0-9]{6})</p>").matcher(json.get("html").asText());
                    if (matcher.find()) codes.put(json.get("to").get(0).asText(), matcher.group(1));
                    if (delayMs.get() > 0) Thread.sleep(delayMs.get());
                    byte[] body = "{\"id\":\"test-message\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status.get(), body.length);
                    exchange.getResponseBody().write(body);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.setExecutor(executor);
            server.start();
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    public String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public String code(String email) {
        String code = codes.get(email);
        if (code == null) throw new AssertionError("No email captured for test account");
        return code;
    }
    public void close() { server.stop(0); executor.shutdownNow(); }
}
