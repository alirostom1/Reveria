package com.reveria.userservice.util;

import com.reveria.userservice.exception.FileValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public final class FileValidationUtil {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    // PNG: 89 50 4E 47
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    // WebP: RIFF....WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private FileValidationUtil() {
    }

    public static void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is required", "file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File size must not exceed 5 MB", "file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileValidationException("Only JPEG, PNG, and WebP images are allowed", "file");
        }

        validateMagicBytes(file);
    }

    private static void validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new FileValidationException("File is too small to be a valid image", "file");
            }

            if (startsWith(header, JPEG_MAGIC)) {
                return;
            }
            if (startsWith(header, PNG_MAGIC)) {
                return;
            }
            if (bytesRead >= 12 && startsWith(header, RIFF_MAGIC) && regionMatches(header, 8, WEBP_MAGIC)) {
                return;
            }

            throw new FileValidationException("File content does not match a valid JPEG, PNG, or WebP image", "file");
        } catch (IOException e) {
            throw new FileValidationException("Unable to read file", "file");
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean regionMatches(byte[] data, int offset, byte[] expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
