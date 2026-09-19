package com.taskmanager.api.notification.service.impl;

import com.taskmanager.api.common.config.AppProperties;
import com.taskmanager.api.common.exception.ApiException;
import com.taskmanager.api.notification.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.util.HtmlUtils;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.List;

@Service
public class ResendEmailService implements EmailService {
    private final AppProperties properties;
    private final RestClient client;

    public ResendEmailService(AppProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(properties.resendBaseUrl()).requestFactory(factory).build();
    }
    public void sendVerification(String email, String code) {
        send(email, code, "Vérifiez votre adresse email");
    }
    public void sendPasswordReset(String email, String code) {
        send(email, code, "Réinitialisez votre mot de passe");
    }
    private void send(String email, String code, String subject) {
        if (properties.resendApiKey() == null || properties.resendApiKey().isBlank()
                || properties.resendFrom() == null || properties.resendFrom().isBlank()) {
            throw unavailable();
        }
        String html = "<!doctype html><html lang=\"fr\"><body style=\"font-family:Arial;color:#123333\">"
            + "<h1 style=\"color:#0A9A9A\">Task Manager</h1><h2>" + HtmlUtils.htmlEscape(subject)
            + "</h2><p>Votre code à usage unique :</p><p style=\"font-size:32px;color:#EA6A54\">"
            + HtmlUtils.htmlEscape(code)
            + "</p><p>Ce code expire dans 10 minutes. Ne le communiquez à personne.</p>"
            + "<p>Si vous n’êtes pas à l’origine de cette demande, ignorez cet email.</p></body></html>";
        try {
            client.post().uri("/emails").header("Authorization", "Bearer " + properties.resendApiKey())
                .body(Map.of("from", properties.resendFrom(), "to", List.of(email), "subject", subject, "html", html))
                .retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw unavailable();
        }
    }
    private ApiException unavailable() {
        return new ApiException(503, "email_unavailable",
            "L’envoi de l’email est indisponible. Le compte est conservé ; réessayez le renvoi après 60 secondes.");
    }
}
