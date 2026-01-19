package com.reveria.userservice.exception;

import com.reveria.userservice.dto.response.ApiError;
import com.reveria.userservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }

        ApiError error = ApiError.builder()
                .code("VALIDATION_ERROR")
                .path(request.getRequestURI())
                .fields(fields)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("INVALID_CREDENTIALS")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email/username or password", error));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("INVALID_CREDENTIALS")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email/username or password", error));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(
            LockedException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_LOCKED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is locked", error));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(
            DisabledException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_DISABLED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is disabled", error));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtAuth(
            JwtAuthenticationException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("JWT_ERROR")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(TokenReuseException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenReuse(
            TokenReuseException ex,
            HttpServletRequest request
    ) {
        log.warn("Token reuse detected: {}", ex.getFamilyId());

        ApiError error = ApiError.builder()
                .code("TOKEN_REUSE_DETECTED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Session invalidated for security reasons. Please login again.", error));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("EMAIL_EXISTS")
                .path(request.getRequestURI())
                .fields(Map.of("email", "Email is already registered"))
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Email already exists", error));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameExists(
            UsernameAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("USERNAME_EXISTS")
                .path(request.getRequestURI())
                .fields(Map.of("username", "Username is already taken"))
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Username already exists", error));
    }

    @ExceptionHandler(MaxSessionsExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSessions(
            MaxSessionsExceededException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("MAX_SESSIONS_EXCEEDED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Maximum number of active sessions reached", error));
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth(
            OAuthException ex,
            HttpServletRequest request
    ) {
        log.error("OAuth error for provider {}: {}", ex.getProvider(), ex.getMessage());

        ApiError error = ApiError.builder()
                .code("OAUTH_ERROR")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("BAD_REQUEST")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("ILLEGAL_STATE")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: ", ex);

        ApiError error = ApiError.builder()
                .code("INTERNAL_ERROR")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", error));
    }
}