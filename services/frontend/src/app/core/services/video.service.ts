import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { DownloadResponse, PageResponse, Video } from '../models/video.model';
import { Comment } from '../models/comment.model';

export interface UploadProgress {
  phase: 'uploading' | 'done' | 'error';
  percent: number;
  video?: Video;
  error?: string;
}

@Injectable({ providedIn: 'root' })
export class VideoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  uploadVideo(channelUuid: string, title: string, description: string, file: File, categorySlug?: string, tags?: string[]) {
    const formData = new FormData();
    formData.append('channelUuid', channelUuid);
    formData.append('title', title);
    formData.append('description', description);
    if (categorySlug) formData.append('categorySlug', categorySlug);
    if (tags?.length) tags.forEach(t => formData.append('tags', t));
    formData.append('file', file);
    return this.http.post<ApiResponse<Video>>(`${this.apiUrl}/api/content/videos`, formData);
  }

  uploadVideoWithProgress(channelUuid: string, title: string, description: string, file: File, categorySlug?: string, tags?: string[]): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('channelUuid', channelUuid);
    formData.append('title', title);
    formData.append('description', description);
    if (categorySlug) formData.append('categorySlug', categorySlug);
    if (tags?.length) tags.forEach(t => formData.append('tags', t));
    formData.append('file', file);

    const subject = new Subject<UploadProgress>();

    const req = new HttpRequest('POST', `${this.apiUrl}/api/content/videos`, formData, {
      reportProgress: true,
    });

    this.http.request(req).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const percent = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
          subject.next({ phase: 'uploading', percent });
        } else if (event.type === HttpEventType.Response) {
          const body = event.body as ApiResponse<Video>;
          subject.next({ phase: 'done', percent: 100, video: body?.data ?? undefined });
          subject.complete();
        }
      },
      error: (err) => {
        subject.next({ phase: 'error', percent: 0, error: err.error?.message || 'Upload failed' });
        subject.complete();
      },
    });

    return subject.asObservable();
  }

  listVideos(page = 0, size = 12, filters?: { category?: string; tag?: string; search?: string; sort?: string }) {
    const params: any = { page, size };
    if (filters?.category) params.category = filters.category;
    if (filters?.tag) params.tag = filters.tag;
    if (filters?.search) params.search = filters.search;
    if (filters?.sort) params.sort = filters.sort;
    return this.http.get<ApiResponse<PageResponse<Video>>>(`${this.apiUrl}/api/content/videos`, { params });
  }

  listCategories() {
    return this.http.get<ApiResponse<{ id: number; name: string; slug: string; iconUrl?: string }[]>>(
      `${this.apiUrl}/api/content/categories`
    );
  }

  listPopularTags() {
    return this.http.get<ApiResponse<{ id: number; name: string; slug: string; usageCount: number }[]>>(
      `${this.apiUrl}/api/content/tags/popular`
    );
  }

  getVideo(uuid: string) {
    return this.http.get<ApiResponse<Video>>(`${this.apiUrl}/api/content/videos/${uuid}`);
  }

  listMyVideos(page = 0, size = 12) {
    return this.http.get<ApiResponse<PageResponse<Video>>>(
      `${this.apiUrl}/api/content/videos/me`,
      { params: { page, size } },
    );
  }

  // --- Likes ---

  likeVideo(uuid: string) {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/api/content/videos/${uuid}/like`, {});
  }

  unlikeVideo(uuid: string) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/content/videos/${uuid}/like`);
  }

  hasLiked(uuid: string) {
    return this.http.get<ApiResponse<boolean>>(`${this.apiUrl}/api/content/videos/${uuid}/liked`);
  }

  // --- Views ---

  recordView(uuid: string, watchDurationSeconds?: number) {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/api/content/videos/${uuid}/view`, null, {
      params: watchDurationSeconds != null ? { watchDuration: watchDurationSeconds } : {},
    });
  }

  // --- Comments ---

  getComments(uuid: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Comment>>>(
      `${this.apiUrl}/api/content/videos/${uuid}/comments`,
      { params: { page, size } },
    );
  }

  addComment(uuid: string, content: string) {
    return this.http.post<ApiResponse<Comment>>(
      `${this.apiUrl}/api/content/videos/${uuid}/comments`,
      { content },
    );
  }

  deleteComment(commentId: number) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/content/videos/comments/${commentId}`);
  }

  // --- Replies ---

  getReplies(commentId: number, page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Comment>>>(
      `${this.apiUrl}/api/content/videos/comments/${commentId}/replies`,
      { params: { page, size } },
    );
  }

  addReply(commentId: number, content: string) {
    return this.http.post<ApiResponse<Comment>>(
      `${this.apiUrl}/api/content/videos/comments/${commentId}/replies`,
      { content },
    );
  }

  // --- Downloads ---

  requestDownload(uuid: string, quality: string) {
    return this.http.post<ApiResponse<DownloadResponse>>(
      `${this.apiUrl}/api/content/videos/${uuid}/download`,
      null,
      { params: { quality } },
    );
  }

  getDownloadUrl(token: string): string {
    return `${this.apiUrl}/api/content/videos/download/${token}`;
  }
}
