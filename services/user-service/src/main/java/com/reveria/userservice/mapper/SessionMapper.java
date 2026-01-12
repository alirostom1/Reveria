package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.model.entity.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "currentSession", ignore = true)
    SessionResponse toResponse(RefreshToken token);
}