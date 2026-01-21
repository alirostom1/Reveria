package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.request.auth.RegisterRequest;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "authProviders", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "privacySettings", ignore = true)
    User toEntity(RegisterRequest request);

    AuthResponse.UserInfo toUserInfo(User user);
}