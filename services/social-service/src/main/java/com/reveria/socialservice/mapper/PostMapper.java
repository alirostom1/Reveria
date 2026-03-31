package com.reveria.socialservice.mapper;

import com.reveria.socialservice.dto.response.*;
import com.reveria.socialservice.model.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "media", source = "media")
    @Mapping(target = "likedByMe", ignore = true)
    PostResponse toResponse(Post post);

    @Mapping(target = "url", ignore = true)
    PostMediaResponse toMediaResponse(PostMedia media);

    CommentResponse toCommentResponse(PostComment comment);

    ReplyResponse toReplyResponse(PostCommentReply reply);

    FriendshipResponse toFriendshipResponse(Friendship friendship);

    FollowResponse toFollowResponse(Follow follow);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "viewedByMe", ignore = true)
    StoryResponse toStoryResponse(Story story);
}
