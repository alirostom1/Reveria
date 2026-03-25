package com.reveria.contentservice.exception;

import com.reveria.contentservice.dto.response.ApiError;
import com.reveria.contentservice.dto.response.ApiResponse;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleVideoNotFound(
            VideoNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("VIDEO_NOT_FOUND")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code(ex.getResourceType().toUpperCase() + "_NOT_FOUND")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex,
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

    @ExceptionHandler(VideoValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleVideoValidation(
            VideoValidationException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("VIDEO_VALIDATION_ERROR")
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
                .fields(Map.of("file", "File size must not exceed 500 MB"))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File size must not exceed 500 MB", error));
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
