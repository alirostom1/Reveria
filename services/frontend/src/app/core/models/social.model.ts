export interface Post {
  uuid: string;
  authorUuid: string;
  content?: string;
  type: 'TEXT' | 'IMAGE' | 'VIDEO_SHARE';
  visibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE';
  sharedVideoUuid?: string;
  likeCount: number;
  commentCount: number;
  media: PostMedia[];
  likedByMe: boolean;
  createdAt: string;
}

export interface PostMedia {
  id: number;
  mediaType: 'IMAGE' | 'VIDEO';
  url: string;
  displayOrder: number;
}

export interface PostComment {
  id: number;
  userUuid: string;
  content: string;
  replies: PostReply[];
  createdAt: string;
}

export interface PostReply {
  id: number;
  userUuid: string;
  content: string;
  createdAt: string;
}

export interface CreatePostRequest {
  content?: string;
  type: 'TEXT' | 'IMAGE' | 'VIDEO_SHARE';
  visibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE';
  sharedVideoUuid?: string;
}

export interface Story {
  uuid: string;
  authorUuid: string;
  mediaType: 'IMAGE' | 'VIDEO';
  url: string;
  durationSeconds?: number;
  viewCount: number;
  viewedByMe: boolean;
  createdAt: string;
  expiresAt: string;
}

export interface Friendship {
  id: number;
  userUuid: string;
  friendUuid: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  requestedAt: string;
  acceptedAt?: string;
}

export interface Follow {
  id: number;
  followerUuid: string;
  followingUuid: string;
  createdAt: string;
}

export interface RelationshipStatus {
  friendshipStatus: 'PENDING' | 'ACCEPTED' | 'DECLINED' | null;
  friendshipId: number | null;
  friendRequestFromMe: boolean;
  following: boolean;
  followedBy: boolean;
  blocked: boolean;
  blockedBy: boolean;
}

export interface PublicUser {
  uuid: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  allowFriendRequests: boolean;
  profileVisibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE';
  createdAt: string;
}
