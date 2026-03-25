import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { LiveStream } from '../models/livestream.model';
import { PageResponse } from '../models/video.model';

@Injectable({ providedIn: 'root' })
export class LiveStreamService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  goLive(channelUuid: string, title: string, description?: string) {
    return this.http.post<ApiResponse<LiveStream>>(
      `${this.apiUrl}/api/content/streams`,
      { channelUuid, title, description },
    );
  }

  getStream(uuid: string) {
    return this.http.get<ApiResponse<LiveStream>>(`${this.apiUrl}/api/content/streams/${uuid}`);
  }

  getActiveStream(channelUuid: string) {
    return this.http.get<ApiResponse<LiveStream | null>>(`${this.apiUrl}/api/content/streams/active/${channelUuid}`);
  }

  listLiveStreams(page = 0, size = 12) {
    return this.http.get<ApiResponse<PageResponse<LiveStream>>>(
      `${this.apiUrl}/api/content/streams/live`,
      { params: { page, size } },
    );
  }

  listChannelStreams(channelUuid: string, page = 0, size = 12) {
    return this.http.get<ApiResponse<PageResponse<LiveStream>>>(
      `${this.apiUrl}/api/content/streams/channel/${channelUuid}`,
      { params: { page, size } },
    );
  }

  endStream(uuid: string) {
    return this.http.post<ApiResponse<LiveStream>>(
      `${this.apiUrl}/api/content/streams/${uuid}/end`,
      {},
    );
  }
}
