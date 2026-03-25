export interface Video {
  uuid: string;
  title: string;
  description?: string;
  status: 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';
  visibility?: string;
  hlsManifestUrl?: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  width?: number;
  height?: number;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  allowComments?: boolean;
  allowDownloads?: boolean;
  userUuid: string;
  channelUuid?: string;
  channelName?: string;
  categoryName?: string;
  tags?: string[];
  renditions?: Rendition[];
  publishedAt?: string;
  createdAt: string;
}

export interface Rendition {
  quality: string;
  hlsPlaylistUrl?: string;
  mp4FileUrl?: string;
  width?: number;
  height?: number;
  bitrate?: number;
  fileSizeBytes?: number;
  status: string;
}

export interface DownloadResponse {
  downloadToken: string;
  downloadUrl: string;
  quality: string;
  fileSizeBytes?: number;
  expiresAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
