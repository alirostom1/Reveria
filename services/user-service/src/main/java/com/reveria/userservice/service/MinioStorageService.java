package com.reveria.userservice.service;

import com.reveria.userservice.config.StorageConfig;
import com.reveria.userservice.exception.StorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageConfig storageConfig;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(storageConfig.getBucket())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(storageConfig.getBucket())
                                .build()
                );
                setPublicReadPolicy();
                log.info("Created MinIO bucket: {}", storageConfig.getBucket());
            }
        } catch (Exception e) {
            throw new StorageException("Failed to initialize MinIO bucket", e);
        }
    }

    private void setPublicReadPolicy() throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(storageConfig.getBucket());

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(storageConfig.getBucket())
                        .config(policy)
                        .build()
        );
    }

    @Override
    public String upload(String path, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageConfig.getBucket())
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            return storageConfig.getEndpoint() + "/" + storageConfig.getBucket() + "/" + path;
        } catch (Exception e) {
            throw new StorageException("Failed to upload file: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageConfig.getBucket())
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + path, e);
        }
    }

    @Override
    public String extractPathFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String prefix = storageConfig.getEndpoint() + "/" + storageConfig.getBucket() + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        return null;
    }
}
