import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SocialService } from '../../../../core/services/social.service';
import { Story, PublicUser } from '../../../../core/models/social.model';

@Component({
  selector: 'app-story-viewer',
  imports: [],
  templateUrl: './story-viewer.html',
})
export class StoryViewer implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly socialService = inject(SocialService);

  protected readonly loading = signal(true);
  protected readonly story = signal<Story | null>(null);
  protected readonly author = signal<PublicUser | null>(null);
  protected readonly progress = signal(0);

  private progressInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const uuid = params.get('uuid');
      if (uuid) this.loadStory(uuid);
    });
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  private loadStory(uuid: string): void {
    this.socialService.getStory(uuid).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.story.set(res.data);
          this.socialService.markStoryViewed(uuid).subscribe();
          this.startProgress(res.data.durationSeconds ?? 5);
          this.socialService.getPublicProfile(res.data.authorUuid).subscribe({
            next: (profileRes) => {
              if (profileRes.data) this.author.set(profileRes.data);
            },
          });
        }
      },
      error: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/social');
      },
    });
  }

  private startProgress(durationSeconds: number): void {
    this.clearTimer();
    const totalMs = durationSeconds * 1000;
    const intervalMs = 50;
    let elapsed = 0;

    this.progressInterval = setInterval(() => {
      elapsed += intervalMs;
      this.progress.set(Math.min((elapsed / totalMs) * 100, 100));
      if (elapsed >= totalMs) {
        this.close();
      }
    }, intervalMs);
  }

  private clearTimer(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  protected close(): void {
    this.clearTimer();
    this.router.navigateByUrl('/social');
  }

  protected timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
  }
}
