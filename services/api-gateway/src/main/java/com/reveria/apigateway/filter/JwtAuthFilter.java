package com.reveria.apigateway.filter;

import com.reveria.apigateway.config.ApiGatewayConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ApiGatewayConfig gatewayProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = extractClaims(token);

            String tokenType = claims.get("tokenType", String.class);
            if (!"ACCESS".equals(tokenType)) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid token type");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-UUID", claims.get("uuid", String.class))
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Roles", String.join(",", extractRoles(claims)))
                    .header("X-Account-Type", claims.get("type", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token has expired");
        } catch (SignatureException e) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid token signature");
        } catch (MalformedJwtException e) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid token format");
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed");
        }
    }

    private boolean isPublicPath(String path) {
        List<String> publicPaths = gatewayProperties.getPublicPaths();
        if (publicPaths == null) return false;
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return roles != null ? roles : List.of();
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = """
                {"success":false,"message":"%s","data":null,"error":{"code":"%s","path":"%s"}}""".formatted(
                message, code, path);

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
