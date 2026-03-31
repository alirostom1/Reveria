import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { SocialService } from '../../../../core/services/social.service';
import { Post, PublicUser } from '../../../../core/models/social.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
})
export class Home implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly socialService = inject(SocialService);

  protected readonly posts = signal<Post[]>([]);
  protected readonly loading = signal(true);
  protected readonly hasMore = signal(false);
  protected readonly profiles = signal<Map<string, PublicUser>>(new Map());
  private page = 0;

  ngOnInit(): void {
    this.loadFeed(true);
  }

  private loadFeed(reset: boolean): void {
    if (reset) this.loading.set(true);
    const req = this.authService.isAuthenticated()
      ? this.socialService.getFeed(this.page, 10)
      : this.socialService.getPublicFeed(this.page, 10);

    req.subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          if (reset) this.posts.set(res.data.content);
          else this.posts.update((prev) => [...prev, ...res.data!.content]);
          this.hasMore.set(res.data.number < res.data.totalPages - 1);
          this.resolveProfiles(res.data.content.map((p) => p.authorUuid));
        }
      },
      error: () => this.loading.set(false),
    });
  }

  protected loadMore(): void {
    this.page++;
    this.loadFeed(false);
  }

  private resolveProfiles(uuids: string[]): void {
    const cache = this.profiles();
    const missing = [...new Set(uuids)].filter((u) => !cache.has(u));
    if (!missing.length) return;

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

  protected timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;
    return `${Math.floor(months / 12)}y ago`;
  }

  protected formatCount(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toString();
  }
}
