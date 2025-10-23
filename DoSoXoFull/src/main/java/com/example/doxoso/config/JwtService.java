// config/JwtService.java
package com.example.doxoso.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final String SECRET_KEY = "thisisaverylongsecretkeyforjwtauthentication123456789"; // >= 32 bytes

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ==== Access token (login) ====
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("typ", "ACCESS"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 60 * 1000)) // 10h
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ==== Reset token (forgot password) ====
    public String generateResetToken(String username) {
        // hiệu lực 10 phút
        long ttlMillis = 10 * 60 * 1000;
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("typ", "RESET"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isResetToken(String token) {
        String typ = extractClaim(token, claims -> (String) claims.get("typ"));
        return "RESET".equals(typ);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaim(token, Claims::getExpiration); // sẽ throw nếu sai/chết hạn
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
