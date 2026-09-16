package org.proj.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.proj.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public long extractTokenVersion(String token) {
        Number version = extractClaim(
                token,
                claims -> claims.get("tokenVersion", Number.class)
        );
        return version == null ? -1L : version.longValue();
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        if (userDetails instanceof UserEntity user) {
            claims.put("userId", user.getId());
            claims.put("email", user.getUsername());
            claims.put("role", user.getRole().getRoleName());
            claims.put(
                    "tokenVersion",
                    user.getTokenVersion() == null
                            ? 0L
                            : user.getTokenVersion()
            );
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(
            Map<String, Object> claims,
            String subject) {

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(
            String token,
            UserDetails userDetails) {

        final String username = extractUsername(token);

        long tokenVersion = extractTokenVersion(token);
        long currentTokenVersion = 0L;

        if (userDetails instanceof UserEntity user
                && user.getTokenVersion() != null) {
            currentTokenVersion = user.getTokenVersion();
        }

        return username.equals(userDetails.getUsername())
                && tokenVersion == currentTokenVersion
                && !isTokenExpired(token);
    }
}
