package com.reveria.userservice.util;

import com.reveria.userservice.exception.FileValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationUtilTest {

    // JPEG magic bytes: FF D8 FF
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

    // PNG magic bytes: 89 50 4E 47
    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    // WebP magic bytes: RIFF....WEBP
    private static final byte[] WEBP_BYTES = {
            0x52, 0x49, 0x46, 0x46,  // RIFF
            0x00, 0x00, 0x00, 0x00,  // file size placeholder
            0x57, 0x45, 0x42, 0x50   // WEBP
    };

    @Test
    void validateAvatar_validJpeg_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", JPEG_BYTES
        );

        assertThatCode(() -> FileValidationUtil.validateAvatar(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAvatar_validPng_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", PNG_BYTES
        );

        assertThatCode(() -> FileValidationUtil.validateAvatar(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAvatar_validWebp_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.webp", "image/webp", WEBP_BYTES
        );

        assertThatCode(() -> FileValidationUtil.validateAvatar(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAvatar_unsupportedContentType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.gif", "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38}
        );

        assertThatThrownBy(() -> FileValidationUtil.validateAvatar(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Only JPEG, PNG, and WebP");
    }

    @Test
    void validateAvatar_emptyFile_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> FileValidationUtil.validateAvatar(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void validateAvatar_nullFile_throws() {
        assertThatThrownBy(() -> FileValidationUtil.validateAvatar(null))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void validateAvatar_wrongMagicBytes_throws() {
        // Content type says JPEG but bytes are random
        byte[] badBytes = {0x00, 0x01, 0x02, 0x03, 0x04};
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", badBytes
        );

        assertThatThrownBy(() -> FileValidationUtil.validateAvatar(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("File content does not match");
    }

    @Test
    void validateAvatar_fileTooLarge_throws() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
        // Set JPEG magic bytes so it passes other checks if size check is removed
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", largeContent
        );

        assertThatThrownBy(() -> FileValidationUtil.validateAvatar(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("must not exceed 5 MB");
    }
}
