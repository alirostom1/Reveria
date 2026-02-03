package com.reveria.userservice.service;

import java.io.InputStream;

public interface StorageService {

    String upload(String path, InputStream inputStream, long size, String contentType);

    void delete(String path);

    String extractPathFromUrl(String url);
}
