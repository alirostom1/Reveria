import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/video.model';
import {
  Post,
  PostComment,
  PostReply,
  Story,
  Friendship,
  Follow,
  CreatePostRequest,
  PublicUser,
  RelationshipStatus,
} from '../models/social.model';

@Injectable({ providedIn: 'root' })
export class SocialService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // ── Users ──

  searchUsers(q: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<PublicUser>>>(
      `${this.apiUrl}/api/users/search`,
      { params: { q, page, size } },
    );
  }

  getPublicProfile(uuid: string) {
    return this.http.get<ApiResponse<PublicUser>>(`${this.apiUrl}/api/users/${uuid}`);
  }

  getRelationshipStatus(targetUuid: string) {
    return this.http.get<ApiResponse<RelationshipStatus>>(
      `${this.apiUrl}/api/social/relationships/status/${targetUuid}`,
    );
  }

  // ── Posts ──

  createPost(request: CreatePostRequest, files?: File[]) {
    const formData = new FormData();
    formData.append('post', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (files?.length) {
      files.forEach((f) => formData.append('files', f));
    }
    return this.http.post<ApiResponse<Post>>(`${this.apiUrl}/api/social/posts`, formData);
  }

  getFeed(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Post>>>(`${this.apiUrl}/api/social/posts/feed`, {
      params: { page, size },
    });
  }

  getPublicFeed(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Post>>>(
      `${this.apiUrl}/api/social/posts/feed/public`,
      { params: { page, size } },
    );
  }

  getUserPosts(userUuid: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Post>>>(
      `${this.apiUrl}/api/social/posts/user/${userUuid}`,
      { params: { page, size } },
    );
  }

  getPost(uuid: string) {
    return this.http.get<ApiResponse<Post>>(`${this.apiUrl}/api/social/posts/${uuid}`);
  }

  updatePost(uuid: string, content: string, visibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE') {
    return this.http.patch<ApiResponse<Post>>(`${this.apiUrl}/api/social/posts/${uuid}`, { content, visibility });
  }

  deletePost(uuid: string) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/social/posts/${uuid}`);
  }

  toggleLike(uuid: string) {
    return this.http.post<ApiResponse<Post>>(`${this.apiUrl}/api/social/posts/${uuid}/like`, {});
  }

  getComments(uuid: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<PostComment>>>(
      `${this.apiUrl}/api/social/posts/${uuid}/comments`,
      { params: { page, size } },
    );
  }

  addComment(uuid: string, content: string) {
    return this.http.post<ApiResponse<PostComment>>(
      `${this.apiUrl}/api/social/posts/${uuid}/comments`,
      { content },
    );
  }

  addReply(commentId: number, content: string) {
    return this.http.post<ApiResponse<PostReply>>(
      `${this.apiUrl}/api/social/posts/comments/${commentId}/replies`,
      { content },
    );
  }

  deleteComment(commentId: number) {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/api/social/posts/comments/${commentId}`,
    );
  }

  // ── Stories ──

  createStory(file: File, durationSeconds?: number) {
    const formData = new FormData();
    formData.append('file', file);
    if (durationSeconds != null) formData.append('durationSeconds', durationSeconds.toString());
    return this.http.post<ApiResponse<Story>>(`${this.apiUrl}/api/social/stories`, formData);
  }

  getFeedStories() {
    return this.http.get<ApiResponse<Story[]>>(`${this.apiUrl}/api/social/stories/feed`);
  }

  getStory(uuid: string) {
    return this.http.get<ApiResponse<Story>>(`${this.apiUrl}/api/social/stories/${uuid}`);
  }

  markStoryViewed(uuid: string) {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/api/social/stories/${uuid}/view`,
      {},
    );
  }

  deleteStory(uuid: string) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/social/stories/${uuid}`);
  }

  // ── Friendships ──

  sendFriendRequest(friendUuid: string) {
    return this.http.post<ApiResponse<Friendship>>(
      `${this.apiUrl}/api/social/relationships/friends/request/${friendUuid}`,
      {},
    );
  }

  acceptFriendRequest(id: number) {
    return this.http.put<ApiResponse<Friendship>>(
      `${this.apiUrl}/api/social/relationships/friends/${id}/accept`,
      {},
    );
  }

  declineFriendRequest(id: number) {
    return this.http.put<ApiResponse<Friendship>>(
      `${this.apiUrl}/api/social/relationships/friends/${id}/decline`,
      {},
    );
  }

  removeFriend(id: number) {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/api/social/relationships/friends/${id}`,
    );
  }

  getFriends(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Friendship>>>(
      `${this.apiUrl}/api/social/relationships/friends`,
      { params: { page, size } },
    );
  }

  getPendingRequests(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Friendship>>>(
      `${this.apiUrl}/api/social/relationships/friends/pending`,
      { params: { page, size } },
    );
  }

  // ── Follows ──

  follow(targetUserUuid: string) {
    return this.http.post<ApiResponse<Follow>>(
      `${this.apiUrl}/api/social/relationships/follow/${targetUserUuid}`,
      {},
    );
  }

  unfollow(targetUserUuid: string) {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/api/social/relationships/follow/${targetUserUuid}`,
    );
  }

  getFollowers(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Follow>>>(
      `${this.apiUrl}/api/social/relationships/followers`,
      { params: { page, size } },
    );
  }

  getFollowing(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<Follow>>>(
      `${this.apiUrl}/api/social/relationships/following`,
      { params: { page, size } },
    );
  }

  // ── Blocks ──

  blockUser(targetUserUuid: string) {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/api/social/relationships/block/${targetUserUuid}`,
      {},
    );
  }

  unblockUser(targetUserUuid: string) {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/api/social/relationships/block/${targetUserUuid}`,
    );
  }

  getBlockedUsers(page = 0, size = 20) {
    return this.http.get<ApiResponse<PageResponse<string>>>(
      `${this.apiUrl}/api/social/relationships/blocked`,
      { params: { page, size } },
    );
  }
}
