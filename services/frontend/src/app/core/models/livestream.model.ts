export interface LiveStream {
  uuid: string;
  hostUuid: string;
  channelUuid: string;
  channelName?: string;
  title: string;
  description?: string;
  hlsPlaybackUrl?: string;
  status: 'IDLE' | 'LIVE' | 'ENDED';
  viewerCount: number;
  startedAt?: string;
  endedAt?: string;
}
