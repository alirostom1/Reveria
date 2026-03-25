import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Channel } from '../models/channel.model';
import { PageResponse, Video } from '../models/video.model';

@Injectable({ providedIn: 'root' })
export class ChannelService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  createChannel(name: string, description?: string) {
    return this.http.post<ApiResponse<Channel>>(`${this.apiUrl}/api/content/channels`, {
      name,
      description,
    });
  }

  getMyChannels() {
    return this.http.get<ApiResponse<Channel[]>>(`${this.apiUrl}/api/content/channels/me`);
  }

  getChannel(uuid: string) {
    return this.http.get<ApiResponse<Channel>>(`${this.apiUrl}/api/content/channels/${uuid}`);
  }

  updateChannel(uuid: string, data: { name?: string; description?: string; bannerUrl?: string; avatarUrl?: string }) {
    return this.http.put<ApiResponse<Channel>>(`${this.apiUrl}/api/content/channels/${uuid}`, data);
  }

  getChannelVideos(channelUuid: string, page = 0, size = 12) {
    return this.http.get<ApiResponse<PageResponse<Video>>>(
      `${this.apiUrl}/api/content/videos/channel/${channelUuid}`,
      { params: { page, size } },
    );
  }

  uploadAvatar(uuid: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<Channel>>(`${this.apiUrl}/api/content/channels/${uuid}/avatar`, formData);
  }

  uploadBanner(uuid: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<Channel>>(`${this.apiUrl}/api/content/channels/${uuid}/banner`, formData);
  }

  regenerateStreamKey(channelUuid: string) {
    return this.http.post<ApiResponse<Channel>>(
      `${this.apiUrl}/api/content/channels/${channelUuid}/regenerate-stream-key`,
      {},
    );
  }

  subscribe(channelUuid: string) {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/api/content/channels/${channelUuid}/subscribe`, {});
  }

  unsubscribe(channelUuid: string) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/content/channels/${channelUuid}/subscribe`);
  }

  isSubscribed(channelUuid: string) {
    return this.http.get<ApiResponse<boolean>>(`${this.apiUrl}/api/content/channels/${channelUuid}/subscribed`);
  }
}
