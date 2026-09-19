package com.taskmanager.api.auth.service.impl;
import com.taskmanager.api.auth.model.CodePurpose;
import com.taskmanager.api.auth.service.SecretService;
import com.taskmanager.api.common.config.AppProperties;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SecretServiceImpl implements SecretService {
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;
    public SecretServiceImpl(AppProperties properties) { key = properties.codeSecret().getBytes(StandardCharsets.UTF_8); }
    public String newCode() { return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
    public String newToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String tokenHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
    public String codeHash(UUID userId, CodePurpose purpose, String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + purpose + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
}
