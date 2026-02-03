package com.reveria.userservice.controller;

import com.reveria.userservice.dto.request.profile.DeactivateAccountRequest;
import com.reveria.userservice.dto.request.profile.UpdatePrivacyRequest;
import com.reveria.userservice.dto.request.profile.UpdateProfileRequest;
import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.LinkedProviderResponse;
import com.reveria.userservice.dto.response.PrivacySettingsResponse;
import com.reveria.userservice.dto.response.UserProfileResponse;
import com.reveria.userservice.security.UserPrincipal;
import com.reveria.userservice.service.ProfileService;
import com.reveria.userservice.util.FileValidationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserProfileResponse profile = profileService.getProfile(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse profile = profileService.updateProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }


    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        FileValidationUtil.validateAvatar(file);
        UserProfileResponse profile = profileService.updateAvatar(principal.getUser().getId(), file);
        return ResponseEntity.ok(ApiResponse.success(profile, "Avatar updated successfully"));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deleteAvatar(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserProfileResponse profile = profileService.deleteAvatar(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Avatar deleted successfully"));
    }


    @GetMapping("/me/privacy")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> getPrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PrivacySettingsResponse settings = profileService.getPrivacySettings(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PatchMapping("/me/privacy")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> updatePrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePrivacyRequest request
    ) {
        PrivacySettingsResponse settings = profileService.updatePrivacySettings(principal.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Privacy settings updated successfully"));
    }

    @GetMapping("/me/providers")
    public ResponseEntity<ApiResponse<List<LinkedProviderResponse>>> getLinkedProviders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<LinkedProviderResponse> providers = profileService.getLinkedProviders(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @PostMapping("/me/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeactivateAccountRequest request
    ) {
        profileService.deactivateAccount(principal.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }
}