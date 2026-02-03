package com.reveria.userservice.exception;

import com.reveria.userservice.dto.response.ApiError;
import com.reveria.userservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.List;
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

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("INVALID_TOKEN")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordMismatch(
            PasswordMismatchException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("PASSWORD_MISMATCH")
                .path(request.getRequestURI())
                .fields(Map.of("currentPassword", "Current password is incorrect"))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_LOCKED")
                .path(request.getRequestURI())
                .details(List.of(
                        "Account is temporarily locked due to too many failed login attempts",
                        "Try again in " + ex.getRemainingMinutes() + " minutes"
                ))
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is temporarily locked", error));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooManyRequests(
            TooManyRequestsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("TOO_MANY_REQUESTS")
                .path(request.getRequestURI())
                .details(List.of("Try again in " + ex.getRetryAfterMinutes() + " minutes"))
                .build();

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterMinutes() * 60))
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

    @ExceptionHandler(ModeratorAccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleModeratorDeactivated(
            ModeratorAccountDeactivatedException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("MODERATOR_ACCOUNT_DEACTIVATED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Moderator account is deactivated", error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("ACCESS_DENIED")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", error));
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileValidation(
            FileValidationException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("FILE_VALIDATION_ERROR")
                .path(request.getRequestURI())
                .fields(Map.of(ex.getField(), ex.getMessage()))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorage(
            StorageException ex,
            HttpServletRequest request
    ) {
        log.error("Storage error: ", ex);

        ApiError error = ApiError.builder()
                .code("STORAGE_ERROR")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("File storage operation failed", error));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("FILE_TOO_LARGE")
                .path(request.getRequestURI())
                .fields(Map.of("file", "File size must not exceed 5 MB"))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File size must not exceed 5 MB", error));
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