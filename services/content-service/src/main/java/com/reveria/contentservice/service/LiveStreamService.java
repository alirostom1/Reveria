package com.reveria.contentservice.service;

import com.reveria.contentservice.config.LiveStreamConfig;
import com.reveria.contentservice.dto.request.CreateLiveStreamRequest;
import com.reveria.contentservice.dto.response.LiveStreamResponse;
import com.reveria.contentservice.dto.response.StreamStatusEvent;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import com.reveria.contentservice.mapper.LiveStreamMapper;
import com.reveria.contentservice.model.entity.Channel;
import com.reveria.contentservice.model.entity.LiveStream;
import com.reveria.contentservice.model.enums.LiveStreamStatus;
import com.reveria.contentservice.repository.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final ChannelService channelService;
    private final LiveStreamMapper liveStreamMapper;
    private final LiveStreamConfig liveStreamConfig;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public LiveStreamResponse goLive(String hostUuid, CreateLiveStreamRequest request) {
        Channel channel = channelService.getChannelEntity(request.getChannelUuid());

        if (!channel.getOwnerUuid().equals(hostUuid)) {
            throw new IllegalStateException("Not the channel owner");
        }

        liveStreamRepository.findByChannelIdAndStatus(channel.getId(), LiveStreamStatus.IDLE)
                .ifPresent(s -> { throw new IllegalStateException("Channel already has a pending stream"); });
        liveStreamRepository.findByChannelIdAndStatus(channel.getId(), LiveStreamStatus.LIVE)
                .ifPresent(s -> { throw new IllegalStateException("Channel is already live"); });

        LiveStream stream = LiveStream.builder()
                .hostUuid(hostUuid)
                .channel(channel)
                .title(request.getTitle())
                .description(request.getDescription())
                .hlsPlaybackUrl(buildHlsPlaybackUrl(channel.getStreamKey()))
                .status(LiveStreamStatus.IDLE)
                .build();
        stream = liveStreamRepository.save(stream);

        log.info("LiveStream {} created for channel {} by user {}", stream.getUuid(), channel.getUuid(), hostUuid);
        return liveStreamMapper.toResponse(stream);
    }

    public LiveStreamResponse getStream(String uuid) {
        LiveStream stream = liveStreamRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("LiveStream", uuid));
        return liveStreamMapper.toResponse(stream);
    }

    public LiveStreamResponse getActiveStream(String channelUuid) {
        Channel channel = channelService.getChannelEntity(channelUuid);
        List<LiveStreamStatus> activeStatuses = List.of(LiveStreamStatus.IDLE, LiveStreamStatus.LIVE);
        return liveStreamRepository.findByChannelIdAndStatusIn(channel.getId(), activeStatuses)
                .stream()
                .findFirst()
                .map(liveStreamMapper::toResponse)
                .orElse(null);
    }

    public Page<LiveStreamResponse> listLiveStreams(Pageable pageable) {
        return liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE, pageable)
                .map(liveStreamMapper::toResponse);
    }

    public Page<LiveStreamResponse> listChannelStreams(String channelUuid, Pageable pageable) {
        Channel channel = channelService.getChannelEntity(channelUuid);
        return liveStreamRepository
                .findByChannelIdAndStatusOrderByStartedAtDesc(channel.getId(), LiveStreamStatus.LIVE, pageable)
                .map(liveStreamMapper::toResponse);
    }

    @Transactional
    public LiveStreamResponse endStream(String uuid, String hostUuid) {
        LiveStream stream = liveStreamRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("LiveStream", uuid));

        if (!stream.getHostUuid().equals(hostUuid)) {
            throw new IllegalStateException("Not the stream host");
        }

        stream.setStatus(LiveStreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        stream = liveStreamRepository.save(stream);

        broadcastAfterCommit(stream);
        log.info("LiveStream {} ended", uuid);
        return liveStreamMapper.toResponse(stream);
    }

    @Transactional
    public boolean onPublish(String streamKey) {
        try {
            Channel channel = channelService.getChannelEntityByStreamKey(streamKey);
            return liveStreamRepository.findByChannelIdAndStatus(channel.getId(), LiveStreamStatus.IDLE)
                    .map(stream -> {
                        stream.setStatus(LiveStreamStatus.LIVE);
                        stream.setStartedAt(LocalDateTime.now());
                        liveStreamRepository.save(stream);
                        broadcastAfterCommit(stream);
                        log.info("Stream {} went LIVE via RTMP publish on channel {}", stream.getUuid(), channel.getUuid());
                        return true;
                    })
                    .orElseGet(() -> {
                        log.warn("No IDLE stream found for channel {} with stream key {}", channel.getUuid(), streamKey);
                        return false;
                    });
        } catch (Exception e) {
            log.warn("RTMP on_publish rejected for stream key {}: {}", streamKey, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void onDone(String streamKey) {
        try {
            Channel channel = channelService.getChannelEntityByStreamKey(streamKey);
            liveStreamRepository.findByChannelIdAndStatus(channel.getId(), LiveStreamStatus.LIVE)
                    .ifPresent(stream -> {
                        stream.setStatus(LiveStreamStatus.ENDED);
                        stream.setEndedAt(LocalDateTime.now());
                        liveStreamRepository.save(stream);
                        broadcastAfterCommit(stream);
                        log.info("Stream {} ended via RTMP on_done on channel {}", stream.getUuid(), channel.getUuid());
                    });
        } catch (Exception e) {
            log.warn("RTMP on_done failed for stream key {}: {}", streamKey, e.getMessage());
        }
    }

    private void broadcastAfterCommit(LiveStream stream) {
        StreamStatusEvent event = StreamStatusEvent.builder()
                .streamUuid(stream.getUuid())
                .channelUuid(stream.getChannel() != null ? stream.getChannel().getUuid() : null)
                .status(stream.getStatus().name())
                .hlsPlaybackUrl(stream.getHlsPlaybackUrl())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/streams/" + stream.getUuid(), event);
                log.debug("Broadcast status {} for stream {}", event.getStatus(), event.getStreamUuid());
            }
        });
    }

    private String buildHlsPlaybackUrl(String streamKey) {
        return liveStreamConfig.getHlsBaseUrl() + "/" + streamKey + ".m3u8";
    }
}
