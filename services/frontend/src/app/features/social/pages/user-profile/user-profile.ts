import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { SocialService } from '../../../../core/services/social.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Post, PublicUser, RelationshipStatus } from '../../../../core/models/social.model';

@Component({
  selector: 'app-user-profile',
  imports: [RouterLink, FormsModule],
  templateUrl: './user-profile.html',
})
export class UserProfile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly socialService = inject(SocialService);
  protected readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly user = signal<PublicUser | null>(null);
  protected readonly status = signal<RelationshipStatus | null>(null);
  protected readonly posts = signal<Post[]>([]);
  protected readonly postsLoading = signal(false);
  protected readonly postsHasMore = signal(false);
  protected readonly isPrivate = signal(false);
  protected readonly isMe = signal(false);
  protected readonly actionLoading = signal(false);

  private postsPage = 0;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const uuid = params.get('uuid');
      if (uuid) this.loadProfile(uuid);
    });
  }

  private loadProfile(uuid: string): void {
    this.loading.set(true);
    this.isMe.set(uuid === this.authService.currentUser()?.uuid);

    this.socialService.getPublicProfile(uuid).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.user.set(res.data);
          if (res.data.profileVisibility === 'PRIVATE') {
            this.isPrivate.set(true);
          } else {
            this.loadPosts(true);
          }
          if (!this.isMe()) {
            this.loadRelationshipStatus(uuid);
          }
        }
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('User not found');
      },
    });
  }

  private loadRelationshipStatus(uuid: string): void {
    this.socialService.getRelationshipStatus(uuid).subscribe({
      next: (res) => {
        if (res.data) this.status.set(res.data);
      },
    });
  }

  private loadPosts(reset: boolean): void {
    const u = this.user();
    if (!u) return;

    this.postsLoading.set(true);
    this.socialService.getUserPosts(u.uuid, this.postsPage, 10).subscribe({
      next: (res) => {
        this.postsLoading.set(false);
        if (res.data) {
          if (reset) this.posts.set(res.data.content);
          else this.posts.update((prev) => [...prev, ...res.data!.content]);
          this.postsHasMore.set(res.data.number < res.data.totalPages - 1);
        }
      },
      error: () => this.postsLoading.set(false),
    });
  }

  // ── Actions ──

  protected sendFriendRequest(): void {
    const u = this.user();
    if (!u) return;
    this.actionLoading.set(true);

    this.socialService.sendFriendRequest(u.uuid).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadRelationshipStatus(u.uuid);
        this.toast.success('Friend request sent');
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Failed to send friend request');
      },
    });
  }

  protected removeFriend(): void {
    const s = this.status();
    if (!s?.friendshipId) return;
    this.actionLoading.set(true);

    this.socialService.removeFriend(s.friendshipId).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadRelationshipStatus(this.user()!.uuid);
        this.toast.success('Friend removed');
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Failed to remove friend');
      },
    });
  }

  protected toggleFollow(): void {
    const u = this.user();
    const s = this.status();
    if (!u || !s) return;
    this.actionLoading.set(true);

    const action: Observable<unknown> = s.following
      ? this.socialService.unfollow(u.uuid)
      : this.socialService.follow(u.uuid);

    action.subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadRelationshipStatus(u.uuid);
        this.toast.success(s.following ? 'Unfollowed' : 'Following');
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Action failed');
      },
    });
  }

  protected toggleBlock(): void {
    const u = this.user();
    const s = this.status();
    if (!u || !s) return;
    this.actionLoading.set(true);

    const action: Observable<unknown> = s.blocked
      ? this.socialService.unblockUser(u.uuid)
      : this.socialService.blockUser(u.uuid);

    action.subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadRelationshipStatus(u.uuid);
        this.toast.success(s.blocked ? 'User unblocked' : 'User blocked');
        if (!s.blocked) {
          // Blocking removes friendship/follows, redirect back
          this.router.navigateByUrl('/social/friends');
        }
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Action failed');
      },
    });
  }

  protected deletePost(uuid: string): void {
    this.socialService.deletePost(uuid).subscribe({
      next: () => {
        this.posts.update((list) => list.filter((p) => p.uuid !== uuid));
        this.toast.success('Post deleted');
      },
      error: () => this.toast.error('Failed to delete post'),
    });
  }

  protected loadMorePosts(): void {
    this.postsPage++;
    this.loadPosts(false);
  }

  protected timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    return `${Math.floor(days / 30)}mo ago`;
  }

  protected formatCount(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toString();
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'long',
      year: 'numeric',
    });
  }
}
