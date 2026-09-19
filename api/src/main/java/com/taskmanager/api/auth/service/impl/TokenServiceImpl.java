package com.taskmanager.api.auth.service.impl;
import com.taskmanager.api.auth.service.TokenService;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Clock;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final JwtEncoder encoder;
    private final Clock clock;
    public String issue(UUID userId, UUID sessionId) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer("taskmanager").subject(userId.toString())
            .audience(java.util.List.of("taskmanager-api")).issuedAt(now).expiresAt(now.plusSeconds(900))
            .claim("sid", sessionId.toString()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
