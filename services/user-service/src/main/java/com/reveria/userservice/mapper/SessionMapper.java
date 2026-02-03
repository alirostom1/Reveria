package com.reveria.userservice.mapper;

import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.model.entity.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "currentSession", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    SessionResponse toResponse(RefreshToken token);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}