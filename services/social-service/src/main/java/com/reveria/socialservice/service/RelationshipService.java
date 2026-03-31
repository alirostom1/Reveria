package com.reveria.socialservice.service;

import com.reveria.socialservice.dto.response.FollowResponse;
import com.reveria.socialservice.dto.response.FriendshipResponse;
import com.reveria.socialservice.dto.response.RelationshipStatusResponse;
import com.reveria.socialservice.exception.ResourceNotFoundException;
import com.reveria.socialservice.mapper.PostMapper;
import com.reveria.socialservice.model.entity.Follow;
import com.reveria.socialservice.model.entity.Friendship;
import com.reveria.socialservice.model.entity.UserBlock;
import com.reveria.socialservice.model.enums.FriendshipStatus;
import com.reveria.socialservice.repository.FollowRepository;
import com.reveria.socialservice.repository.FriendshipRepository;
import com.reveria.socialservice.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelationshipService {

    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final PostMapper postMapper;

    // ── Status ──

    public RelationshipStatusResponse getRelationshipStatus(String userUuid, String targetUuid) {
        var friendship = friendshipRepository.findBetween(userUuid, targetUuid);
        String friendshipStatus = null;
        Long friendshipId = null;
        boolean fromMe = false;

        if (friendship.isPresent()) {
            var f = friendship.get();
            friendshipStatus = f.getStatus().name();
            friendshipId = f.getId();
            fromMe = f.getUserUuid().equals(userUuid);
        }

        return RelationshipStatusResponse.builder()
                .friendshipStatus(friendshipStatus)
                .friendshipId(friendshipId)
                .friendRequestFromMe(fromMe)
                .following(followRepository.existsByFollowerUuidAndFollowingUuid(userUuid, targetUuid))
                .followedBy(followRepository.existsByFollowerUuidAndFollowingUuid(targetUuid, userUuid))
                .blocked(blockRepository.existsByBlockerUuidAndBlockedUuid(userUuid, targetUuid))
                .blockedBy(blockRepository.existsByBlockerUuidAndBlockedUuid(targetUuid, userUuid))
                .build();
    }

    // ── Friendships ──

    @Transactional
    public FriendshipResponse sendFriendRequest(String userUuid, String friendUuid) {
        if (userUuid.equals(friendUuid)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        if (blockRepository.existsByBlockerUuidAndBlockedUuid(friendUuid, userUuid)) {
            throw new IllegalArgumentException("Cannot send friend request");
        }
        var existing = friendshipRepository.findBetween(userUuid, friendUuid);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Friendship already exists");
        }

        Friendship friendship = Friendship.builder()
                .userUuid(userUuid)
                .friendUuid(friendUuid)
                .status(FriendshipStatus.PENDING)
                .build();
        friendship = friendshipRepository.save(friendship);
        return postMapper.toFriendshipResponse(friendship);
    }

    @Transactional
    public FriendshipResponse acceptFriendRequest(Long id, String userUuid) {
        Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (!friendship.getFriendUuid().equals(userUuid)) {
            throw new IllegalArgumentException("Not your friend request to accept");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setAcceptedAt(LocalDateTime.now());
        friendship = friendshipRepository.save(friendship);
        return postMapper.toFriendshipResponse(friendship);
    }

    @Transactional
    public FriendshipResponse declineFriendRequest(Long id, String userUuid) {
        Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (!friendship.getFriendUuid().equals(userUuid)) {
            throw new IllegalArgumentException("Not your friend request to decline");
        }
        friendship.setStatus(FriendshipStatus.DECLINED);
        friendship = friendshipRepository.save(friendship);
        return postMapper.toFriendshipResponse(friendship);
    }

    @Transactional
    public void removeFriend(Long id, String userUuid) {
        Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));
        if (!friendship.getUserUuid().equals(userUuid) && !friendship.getFriendUuid().equals(userUuid)) {
            throw new IllegalArgumentException("Not part of this friendship");
        }
        friendshipRepository.delete(friendship);
    }

    public Page<FriendshipResponse> getFriends(String userUuid, int page, int size) {
        return friendshipRepository.findByUserUuidOrFriendUuidAndStatus(userUuid, FriendshipStatus.ACCEPTED,
                        PageRequest.of(page, size))
                .map(postMapper::toFriendshipResponse);
    }

    public Page<FriendshipResponse> getPendingRequests(String userUuid, int page, int size) {
        return friendshipRepository.findPendingRequests(userUuid, PageRequest.of(page, size))
                .map(postMapper::toFriendshipResponse);
    }

    // ── Follows ──

    @Transactional
    public FollowResponse follow(String followerUuid, String followingUuid) {
        if (followerUuid.equals(followingUuid)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        if (blockRepository.existsByBlockerUuidAndBlockedUuid(followingUuid, followerUuid)) {
            throw new IllegalArgumentException("Cannot follow this user");
        }
        if (followRepository.existsByFollowerUuidAndFollowingUuid(followerUuid, followingUuid)) {
            throw new IllegalArgumentException("Already following");
        }

        Follow follow = Follow.builder()
                .followerUuid(followerUuid)
                .followingUuid(followingUuid)
                .build();
        follow = followRepository.save(follow);
        return postMapper.toFollowResponse(follow);
    }

    @Transactional
    public void unfollow(String followerUuid, String followingUuid) {
        Follow follow = followRepository.findByFollowerUuidAndFollowingUuid(followerUuid, followingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Not following this user"));
        followRepository.delete(follow);
    }

    public Page<FollowResponse> getFollowers(String userUuid, int page, int size) {
        return followRepository.findByFollowingUuid(userUuid,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(postMapper::toFollowResponse);
    }

    public Page<FollowResponse> getFollowing(String userUuid, int page, int size) {
        return followRepository.findByFollowerUuid(userUuid,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(postMapper::toFollowResponse);
    }

    // ── Blocks ──

    @Transactional
    public void blockUser(String blockerUuid, String blockedUuid) {
        if (blockerUuid.equals(blockedUuid)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        if (blockRepository.existsByBlockerUuidAndBlockedUuid(blockerUuid, blockedUuid)) return;

        // Remove any existing friendship
        friendshipRepository.findBetween(blockerUuid, blockedUuid)
                .ifPresent(friendshipRepository::delete);

        // Remove any follows
        followRepository.findByFollowerUuidAndFollowingUuid(blockerUuid, blockedUuid)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerUuidAndFollowingUuid(blockedUuid, blockerUuid)
                .ifPresent(followRepository::delete);

        UserBlock block = UserBlock.builder()
                .blockerUuid(blockerUuid)
                .blockedUuid(blockedUuid)
                .build();
        blockRepository.save(block);
    }

    @Transactional
    public void unblockUser(String blockerUuid, String blockedUuid) {
        UserBlock block = blockRepository.findByBlockerUuidAndBlockedUuid(blockerUuid, blockedUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Block not found"));
        blockRepository.delete(block);
    }

    public Page<String> getBlockedUsers(String userUuid, int page, int size) {
        return blockRepository.findByBlockerUuid(userUuid, PageRequest.of(page, size))
                .map(UserBlock::getBlockedUuid);
    }
}
