import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SocialService } from '../../../../core/services/social.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Friendship, Follow, PublicUser } from '../../../../core/models/social.model';

type Tab = 'search' | 'friends' | 'pending' | 'followers' | 'following' | 'blocked';

@Component({
  selector: 'app-friends',
  imports: [FormsModule, RouterLink],
  templateUrl: './friends.html',
})
export class Friends implements OnInit {
  private readonly socialService = inject(SocialService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly activeTab = signal<Tab>('friends');
  protected readonly loading = signal(true);

  // Profile cache: uuid → PublicUser
  protected readonly profiles = signal<Map<string, PublicUser>>(new Map());

  // Search
  protected searchQuery = '';
  protected readonly searching = signal(false);
  protected readonly searchResults = signal<PublicUser[]>([]);
  protected readonly searchHasMore = signal(false);
  private searchPage = 0;

  // Lists
  protected readonly friends = signal<Friendship[]>([]);
  protected readonly friendsHasMore = signal(false);
  protected readonly pending = signal<Friendship[]>([]);
  protected readonly pendingHasMore = signal(false);
  protected readonly followers = signal<Follow[]>([]);
  protected readonly followersHasMore = signal(false);
  protected readonly following = signal<Follow[]>([]);
  protected readonly followingHasMore = signal(false);
  protected readonly blocked = signal<string[]>([]);
  protected readonly blockedHasMore = signal(false);

  private pages: Record<string, number> = { friends: 0, pending: 0, followers: 0, following: 0, blocked: 0 };

  ngOnInit(): void {
    this.loadTab('friends', true);
  }

  protected switchTab(tab: Tab): void {
    this.activeTab.set(tab);
    if (tab !== 'search') {
      this.pages[tab] = 0;
      this.loadTab(tab, true);
    }
  }

  // ── Search ──

  protected onSearch(): void {
    const q = this.searchQuery.trim();
    if (q.length < 2) return;

    this.activeTab.set('search');
    this.searchPage = 0;
    this.searching.set(true);

    this.socialService.searchUsers(q, 0).subscribe({
      next: (res) => {
        this.searching.set(false);
        if (res.data) {
          this.searchResults.set(res.data.content);
          this.searchHasMore.set(res.data.number < res.data.totalPages - 1);
        }
      },
      error: () => this.searching.set(false),
    });
  }

  protected loadMoreSearch(): void {
    this.searchPage++;
    this.socialService.searchUsers(this.searchQuery.trim(), this.searchPage).subscribe({
      next: (res) => {
        if (res.data) {
          this.searchResults.update((prev) => [...prev, ...res.data!.content]);
          this.searchHasMore.set(res.data.number < res.data.totalPages - 1);
        }
      },
    });
  }

  protected clearSearch(): void {
    this.searchQuery = '';
    this.searchResults.set([]);
    this.switchTab('friends');
  }

  // ── Tab loading ──

  protected loadMore(): void {
    const tab = this.activeTab();
    if (tab === 'search') {
      this.loadMoreSearch();
      return;
    }
    this.pages[tab]++;
    this.loadTab(tab, false);
  }

  private loadTab(tab: string, reset: boolean): void {
    if (reset) this.loading.set(true);
    const page = this.pages[tab];

    switch (tab) {
      case 'friends':
        this.socialService.getFriends(page).subscribe({
          next: (res) => {
            this.loading.set(false);
            if (res.data) {
              if (reset) this.friends.set(res.data.content);
              else this.friends.update((prev) => [...prev, ...res.data!.content]);
              this.friendsHasMore.set(res.data.number < res.data.totalPages - 1);
              this.resolveProfiles(res.data.content.map((f) => this.otherUuid(f)));
            }
          },
          error: () => this.loading.set(false),
        });
        break;
      case 'pending':
        this.socialService.getPendingRequests(page).subscribe({
          next: (res) => {
            this.loading.set(false);
            if (res.data) {
              if (reset) this.pending.set(res.data.content);
              else this.pending.update((prev) => [...prev, ...res.data!.content]);
              this.pendingHasMore.set(res.data.number < res.data.totalPages - 1);
              this.resolveProfiles(res.data.content.map((f) => f.userUuid));
            }
          },
          error: () => this.loading.set(false),
        });
        break;
      case 'followers':
        this.socialService.getFollowers(page).subscribe({
          next: (res) => {
            this.loading.set(false);
            if (res.data) {
              if (reset) this.followers.set(res.data.content);
              else this.followers.update((prev) => [...prev, ...res.data!.content]);
              this.followersHasMore.set(res.data.number < res.data.totalPages - 1);
              this.resolveProfiles(res.data.content.map((f) => f.followerUuid));
            }
          },
          error: () => this.loading.set(false),
        });
        break;
      case 'following':
        this.socialService.getFollowing(page).subscribe({
          next: (res) => {
            this.loading.set(false);
            if (res.data) {
              if (reset) this.following.set(res.data.content);
              else this.following.update((prev) => [...prev, ...res.data!.content]);
              this.followingHasMore.set(res.data.number < res.data.totalPages - 1);
              this.resolveProfiles(res.data.content.map((f) => f.followingUuid));
            }
          },
          error: () => this.loading.set(false),
        });
        break;
      case 'blocked':
        this.socialService.getBlockedUsers(page).subscribe({
          next: (res) => {
            this.loading.set(false);
            if (res.data) {
              if (reset) this.blocked.set(res.data.content);
              else this.blocked.update((prev) => [...prev, ...res.data!.content]);
              this.blockedHasMore.set(res.data.number < res.data.totalPages - 1);
              this.resolveProfiles(res.data.content);
            }
          },
          error: () => this.loading.set(false),
        });
        break;
    }
  }

  // ── Actions ──

  protected acceptRequest(f: Friendship): void {
    this.socialService.acceptFriendRequest(f.id).subscribe({
      next: () => {
        this.pending.update((list) => list.filter((p) => p.id !== f.id));
        this.toast.success('Friend request accepted');
      },
    });
  }

  protected declineRequest(f: Friendship): void {
    this.socialService.declineFriendRequest(f.id).subscribe({
      next: () => {
        this.pending.update((list) => list.filter((p) => p.id !== f.id));
        this.toast.success('Friend request declined');
      },
    });
  }

  protected removeFriend(f: Friendship): void {
    this.socialService.removeFriend(f.id).subscribe({
      next: () => {
        this.friends.update((list) => list.filter((fr) => fr.id !== f.id));
        this.toast.success('Friend removed');
      },
    });
  }

  protected unfollowUser(f: Follow): void {
    this.socialService.unfollow(f.followingUuid).subscribe({
      next: () => {
        this.following.update((list) => list.filter((fl) => fl.id !== f.id));
        this.toast.success('Unfollowed');
      },
    });
  }

  protected unblockUser(uuid: string): void {
    this.socialService.unblockUser(uuid).subscribe({
      next: () => {
        this.blocked.update((list) => list.filter((u) => u !== uuid));
        this.toast.success('User unblocked');
      },
    });
  }

  // ── Friendship helpers ──

  protected otherUuid(f: Friendship): string {
    const me = this.authService.currentUser()?.uuid;
    return f.userUuid === me ? f.friendUuid : f.userUuid;
  }

  // ── Profile resolution ──

  private resolveProfiles(uuids: string[]): void {
    const cache = this.profiles();
    const missing = [...new Set(uuids)].filter((u) => !cache.has(u));
    if (missing.length === 0) return;

    forkJoin(missing.map((uuid) => this.socialService.getPublicProfile(uuid))).subscribe({
      next: (responses) => {
        this.profiles.update((map) => {
          const next = new Map(map);
          responses.forEach((res) => {
            if (res.data) next.set(res.data.uuid, res.data);
          });
          return next;
        });
      },
    });
  }

  protected profile(uuid: string): PublicUser | undefined {
    return this.profiles().get(uuid);
  }

  // ── Helpers ──

  protected timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    return `${Math.floor(days / 30)}mo ago`;
  }

  protected hasMoreForTab(): boolean {
    switch (this.activeTab()) {
      case 'search': return this.searchHasMore();
      case 'friends': return this.friendsHasMore();
      case 'pending': return this.pendingHasMore();
      case 'followers': return this.followersHasMore();
      case 'following': return this.followingHasMore();
      case 'blocked': return this.blockedHasMore();
    }
  }
}
