package com.reveria.contentservice.controller;

import com.reveria.contentservice.dto.response.ApiResponse;
import com.reveria.contentservice.model.entity.VideoCategory;
import com.reveria.contentservice.model.entity.VideoTag;
import com.reveria.contentservice.repository.VideoCategoryRepository;
import com.reveria.contentservice.repository.VideoTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class CategoryController {

    private final VideoCategoryRepository categoryRepository;
    private final VideoTagRepository tagRepository;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<VideoCategory>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                categoryRepository.findAllByOrderByDisplayOrderAsc()));
    }

    @GetMapping("/tags/popular")
    public ResponseEntity<ApiResponse<List<VideoTag>>> listPopularTags() {
        return ResponseEntity.ok(ApiResponse.success(
                tagRepository.findTop20ByOrderByUsageCountDesc()));
    }
}
