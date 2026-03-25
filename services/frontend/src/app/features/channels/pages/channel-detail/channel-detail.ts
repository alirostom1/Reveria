import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChannelService } from '../../../../core/services/channel.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Channel } from '../../../../core/models/channel.model';
import { Video } from '../../../../core/models/video.model';

@Component({
  selector: 'app-channel-detail',
  imports: [RouterLink],
  templateUrl: './channel-detail.html',
})
export class ChannelDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly channelService = inject(ChannelService);
  protected readonly authService = inject(AuthService);

  protected readonly loading = signal(true);
  protected readonly channel = signal<Channel | null>(null);
  protected readonly videos = signal<Video[]>([]);
  protected readonly hasMore = signal(false);
  protected readonly subscribed = signal(false);
  protected readonly subscribing = signal(false);
  protected readonly isOwner = signal(false);
  private page = 0;
  private channelUuid = '';

  ngOnInit(): void {
    this.channelUuid = this.route.snapshot.params['uuid'];
    this.loadChannel();
    this.loadVideos();
  }

  toggleSubscribe(): void {
    if (this.subscribing()) return;
    this.subscribing.set(true);

    const action = this.subscribed()
      ? this.channelService.unsubscribe(this.channelUuid)
      : this.channelService.subscribe(this.channelUuid);

    action.subscribe({
      next: () => {
        const wasSub = this.subscribed();
        this.subscribed.set(!wasSub);
        this.channel.update((ch) =>
          ch ? { ...ch, subscriberCount: ch.subscriberCount + (wasSub ? -1 : 1) } : ch,
        );
        this.subscribing.set(false);
      },
      error: () => this.subscribing.set(false),
    });
  }

  loadMore(): void {
    this.page++;
    this.loadVideos();
  }

  protected formatDuration(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  protected formatCount(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toString();
  }

  private loadChannel(): void {
    this.channelService.getChannel(this.channelUuid).subscribe({
      next: (res) => {
        this.channel.set(res.data ?? null);
        this.loading.set(false);

        const user = this.authService.currentUser();
        if (res.data && user) {
          this.isOwner.set(res.data.ownerUuid === user.uuid);
          if (!this.isOwner()) {
            this.channelService.isSubscribed(this.channelUuid).subscribe({
              next: (subRes) => this.subscribed.set(subRes.data ?? false),
            });
          }
        }
      },
      error: () => this.loading.set(false),
    });
  }

  private loadVideos(): void {
    this.channelService.getChannelVideos(this.channelUuid, this.page, 12).subscribe({
      next: (res) => {
        if (res.data) {
          this.videos.update((prev) => [...prev, ...res.data!.content]);
          this.hasMore.set(res.data.number < res.data.totalPages - 1);
        }
      },
    });
  }
}
