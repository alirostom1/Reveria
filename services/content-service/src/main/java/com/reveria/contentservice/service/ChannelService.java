package com.reveria.contentservice.service;

import com.reveria.contentservice.dto.request.CreateChannelRequest;
import com.reveria.contentservice.dto.request.UpdateChannelRequest;
import com.reveria.contentservice.dto.response.ChannelResponse;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import com.reveria.contentservice.exception.VideoValidationException;
import com.reveria.contentservice.mapper.ChannelMapper;
import com.reveria.contentservice.model.entity.Channel;
import com.reveria.contentservice.model.entity.ChannelSubscription;
import com.reveria.contentservice.repository.ChannelRepository;
import com.reveria.contentservice.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelSubscriptionRepository subscriptionRepository;
    private final ChannelMapper channelMapper;
    private final MinioStorageService storageService;

    private static final int MAX_CHANNELS_PER_USER = 3;

    @Transactional
    public ChannelResponse createChannel(String ownerUuid, CreateChannelRequest request) {
        long count = channelRepository.countByOwnerUuid(ownerUuid);
        if (count >= MAX_CHANNELS_PER_USER) {
            throw new IllegalStateException("Maximum of " + MAX_CHANNELS_PER_USER + " channels allowed per user");
        }

        Channel channel = Channel.builder()
                .ownerUuid(ownerUuid)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        channel = channelRepository.save(channel);

        log.info("Channel {} created by user {}", channel.getUuid(), ownerUuid);
        return channelMapper.toResponse(channel);
    }

    public ChannelResponse getChannel(String uuid) {
        Channel channel = channelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", uuid));
        return channelMapper.toResponse(channel);
    }

    public ChannelResponse getMyChannel(String ownerUuid) {
        Channel channel = channelRepository.findByOwnerUuid(ownerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "owner:" + ownerUuid));
        return channelMapper.toOwnerResponse(channel);
    }

    public List<ChannelResponse> listMyChannels(String ownerUuid) {
        return channelRepository.findAllByOwnerUuid(ownerUuid).stream()
                .map(channelMapper::toOwnerResponse)
                .toList();
    }

    @Transactional
    public ChannelResponse updateChannel(String uuid, String ownerUuid, UpdateChannelRequest request) {
        Channel channel = channelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", uuid));

        if (!channel.getOwnerUuid().equals(ownerUuid)) {
            throw new IllegalStateException("Not the channel owner");
        }

        if (request.getName() != null) channel.setName(request.getName());
        if (request.getDescription() != null) channel.setDescription(request.getDescription());
        if (request.getBannerUrl() != null) channel.setBannerUrl(request.getBannerUrl());
        if (request.getAvatarUrl() != null) channel.setAvatarUrl(request.getAvatarUrl());

        channel = channelRepository.save(channel);
        return channelMapper.toResponse(channel);
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;   // 5 MB
    private static final long MAX_BANNER_SIZE = 10 * 1024 * 1024;  // 10 MB

    @Transactional
    public ChannelResponse uploadImage(String uuid, String ownerUuid, MultipartFile file, String type) {
        Channel channel = channelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", uuid));

        if (!channel.getOwnerUuid().equals(ownerUuid)) {
            throw new IllegalStateException("Not the channel owner");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new VideoValidationException("file", "Unsupported image format. Allowed: JPEG, PNG, WebP, GIF");
        }

        long maxSize = "avatar".equals(type) ? MAX_AVATAR_SIZE : MAX_BANNER_SIZE;
        if (file.getSize() > maxSize) {
            throw new VideoValidationException("file", "File too large. Maximum size: " + (maxSize / 1024 / 1024) + " MB");
        }

        String extension = getImageExtension(contentType);
        String path = "channels/" + uuid + "/" + type + extension;

        try {
            String url = storageService.upload(path, file.getInputStream(), file.getSize(), contentType);

            if ("avatar".equals(type)) {
                channel.setAvatarUrl(url);
            } else {
                channel.setBannerUrl(url);
            }

            channel = channelRepository.save(channel);
            log.info("Channel {} {} updated", uuid, type);
            return channelMapper.toResponse(channel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    private String getImageExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    @Transactional
    public void subscribe(String channelUuid, String subscriberUuid) {
        Channel channel = channelRepository.findByUuid(channelUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelUuid));

        if (channel.getOwnerUuid().equals(subscriberUuid)) {
            throw new IllegalStateException("Cannot subscribe to own channel");
        }

        if (subscriptionRepository.existsByChannelIdAndSubscriberUuid(channel.getId(), subscriberUuid)) {
            return;
        }

        ChannelSubscription sub = ChannelSubscription.builder()
                .channelId(channel.getId())
                .subscriberUuid(subscriberUuid)
                .build();
        subscriptionRepository.save(sub);
        channelRepository.incrementSubscriberCount(channel.getId());

        log.info("User {} subscribed to channel {}", subscriberUuid, channelUuid);
    }

    @Transactional
    public void unsubscribe(String channelUuid, String subscriberUuid) {
        Channel channel = channelRepository.findByUuid(channelUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelUuid));

        subscriptionRepository.findByChannelIdAndSubscriberUuid(channel.getId(), subscriberUuid)
                .ifPresent(sub -> {
                    subscriptionRepository.delete(sub);
                    channelRepository.decrementSubscriberCount(channel.getId());
                    log.info("User {} unsubscribed from channel {}", subscriberUuid, channelUuid);
                });
    }

    public boolean isSubscribed(String channelUuid, String userUuid) {
        Channel channel = channelRepository.findByUuid(channelUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelUuid));
        return subscriptionRepository.existsByChannelIdAndSubscriberUuid(channel.getId(), userUuid);
    }

    public Page<ChannelSubscription> getSubscribers(String channelUuid, Pageable pageable) {
        Channel channel = channelRepository.findByUuid(channelUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelUuid));
        return subscriptionRepository.findByChannelIdOrderBySubscribedAtDesc(channel.getId(), pageable);
    }

    public Channel getChannelEntity(String uuid) {
        return channelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", uuid));
    }

    public Channel getChannelEntityByOwner(String ownerUuid) {
        return channelRepository.findByOwnerUuid(ownerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "owner:" + ownerUuid));
    }

    public Channel getChannelEntityByStreamKey(String streamKey) {
        return channelRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "streamKey:" + streamKey));
    }

    @Transactional
    public ChannelResponse regenerateStreamKey(String uuid, String ownerUuid) {
        Channel channel = channelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", uuid));

        if (!channel.getOwnerUuid().equals(ownerUuid)) {
            throw new IllegalStateException("Not the channel owner");
        }

        channel.setStreamKey(java.util.UUID.randomUUID().toString().replace("-", ""));
        channel = channelRepository.save(channel);

        log.info("Stream key regenerated for channel {}", uuid);
        return channelMapper.toOwnerResponse(channel);
    }
}
