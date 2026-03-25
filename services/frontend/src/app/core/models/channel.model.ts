export interface Channel {
  uuid: string;
  ownerUuid: string;
  name: string;
  description: string;
  bannerUrl: string;
  avatarUrl: string;
  status: string;
  subscriberCount: number;
  streamKey?: string;
  rtmpIngestUrl?: string;
  createdAt: string;
}
