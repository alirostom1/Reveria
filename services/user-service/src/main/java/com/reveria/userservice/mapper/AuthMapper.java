package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    default AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(toUserInfo(user))
                .build();
    }

    AuthResponse.UserInfo toUserInfo(User user);
}