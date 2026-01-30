package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.response.ModeratorAuthResponse;
import com.reveria.userservice.dto.response.ModeratorResponse;
import com.reveria.userservice.model.entity.Moderator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ModeratorAuthMapper {

    default ModeratorAuthResponse toAuthResponse(Moderator moderator, String accessToken,
                                                  String refreshToken, Long expiresIn) {
        return ModeratorAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .moderator(toModeratorInfo(moderator))
                .build();
    }

    ModeratorAuthResponse.ModeratorInfo toModeratorInfo(Moderator moderator);

    @Mapping(target = "active", source = "active")
    ModeratorResponse toModeratorResponse(Moderator moderator);
}
