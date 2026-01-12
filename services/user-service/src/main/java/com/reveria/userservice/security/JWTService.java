package com.reveria.userservice.security;

import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class JWTService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${spring.application.name}")
    private String applicationName;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // TOKEN GENERATION

    public String generateAccessToken(UserDetails userDetails, String uuid, AccountType accountType,String familyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uuid", uuid);
        claims.put("type", accountType.name());
        claims.put("roles", extractRoles(userDetails));
        claims.put("tokenType", "ACCESS");
        claims.put("familyId", familyId);

        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails, String uuid, AccountType accountType,
                                       String familyId, int generation) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uuid", uuid);
        claims.put("type", accountType.name());
        claims.put("tokenType", "REFRESH");
        claims.put("familyId", familyId);
        claims.put("generation", generation);

        return buildToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    public String generateNewFamilyId() {
        return UUID.randomUUID().toString();
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .claims(claims)
                .subject(subject)
                .issuer(applicationName)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    private List<String> extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    //  TOKEN VALIDATION

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtAuthenticationException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(extractTokenType(token));
    }

    public void validateAccessToken(String token) {
        validateToken(token);
        if (!isAccessToken(token)) {
            throw new JwtAuthenticationException("Token is not an access token");
        }
    }

    public void validateRefreshToken(String token) {
        validateToken(token);
        if (!isRefreshToken(token)) {
            throw new JwtAuthenticationException("Token is not a refresh token");
        }
    }

    private void validateToken(String token) {
        try {
            extractAllClaims(token);
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Token has expired");
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Invalid token signature");
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid token format");
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Unsupported token");
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("Token is empty");
        }
    }

    // CLAIM EXTRACTION

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUuid(String token) {
        return extractClaim(token, claims -> claims.get("uuid", String.class));
    }

    public AccountType extractAccountType(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return AccountType.valueOf(type);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public String extractFamilyId(String token) {
        return extractClaim(token, claims -> claims.get("familyId", String.class));
    }

    public Integer extractGeneration(String token) {
        return extractClaim(token, claims -> claims.get("generation", Integer.class));
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // GETTERS

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}