package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.response.LinkedProviderResponse;
import com.reveria.userservice.dto.response.PrivacySettingsResponse;
import com.reveria.userservice.dto.response.UserProfileResponse;
import com.reveria.userservice.model.entity.AuthProvider;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.entity.UserPrivacySettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "linkedProviders", source = "authProviders", qualifiedByName = "mapProviders")
    UserProfileResponse toProfileResponse(User user);

    @Mapping(target = "showOnlineStatus", source = "showOnlineStatus")
    @Mapping(target = "allowDirectMessages", source = "allowDirectMessages")
    @Mapping(target = "allowFriendRequests", source = "allowFriendRequests")
    PrivacySettingsResponse toPrivacyResponse(UserPrivacySettings settings);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Named("mapProviders")
    default List<String> mapProviders(List<AuthProvider> providers) {
        if (providers == null) return List.of();
        return providers.stream()
                .map(p -> p.getProvider().name())
                .toList();
    }

    default LinkedProviderResponse toLinkedProviderResponse(AuthProvider provider, boolean canUnlink) {
        return LinkedProviderResponse.builder()
                .provider(provider.getProvider())
                .linkedAt(formatDateTime(provider.getLinkedAt()))
                .canUnlink(canUnlink)
                .build();
    }
}