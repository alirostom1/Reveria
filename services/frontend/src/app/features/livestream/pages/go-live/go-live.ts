import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LiveStreamService } from '../../../../core/services/livestream.service';
import { ChannelService } from '../../../../core/services/channel.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { Channel } from '../../../../core/models/channel.model';
import { LiveStream } from '../../../../core/models/livestream.model';
import { VideoPlayer } from '../../../../shared/components/video-player/video-player';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-go-live',
  imports: [FormsModule, RouterLink, VideoPlayer],
  templateUrl: './go-live.html',
})
export class GoLive implements OnInit, OnDestroy {
  private readonly liveStreamService = inject(LiveStreamService);
  private readonly channelService = inject(ChannelService);
  private readonly wsService = inject(WebSocketService);

  private wsSub: Subscription | null = null;

  protected readonly channels = signal<Channel[]>([]);
  protected readonly selectedChannelUuid = signal('');
  protected readonly stream = signal<LiveStream | null>(null);
  protected readonly loading = signal(true);
  protected readonly creating = signal(false);
  protected readonly ending = signal(false);
  protected readonly keyVisible = signal(false);
  protected readonly keyCopied = signal(false);
  protected readonly urlCopied = signal(false);

  protected streamTitle = '';
  protected description = '';

  protected readonly selectedChannel = computed(() => {
    const uuid = this.selectedChannelUuid();
    return this.channels().find((ch) => ch.uuid === uuid) ?? null;
  });

  ngOnInit(): void {
    this.channelService.getMyChannels().subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.channels.set(res.data);
          this.selectedChannelUuid.set(res.data[0].uuid);
          this.checkActiveStream(res.data[0].uuid);
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  ngOnDestroy(): void {
    this.unsubscribeWs();
  }

  onChannelChange(uuid: string): void {
    this.selectedChannelUuid.set(uuid);
    this.stream.set(null);
    this.unsubscribeWs();
    this.checkActiveStream(uuid);
  }

  private checkActiveStream(channelUuid: string): void {
    this.loading.set(true);
    this.liveStreamService.getActiveStream(channelUuid).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.stream.set(res.data);
          this.subscribeToStream(res.data.uuid);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  goLive(): void {
    const uuid = this.selectedChannelUuid();
    if (!this.streamTitle.trim() || !uuid || this.creating()) return;
    this.creating.set(true);

    this.liveStreamService
      .goLive(uuid, this.streamTitle.trim(), this.description.trim() || undefined)
      .subscribe({
        next: (res) => {
          this.creating.set(false);
          if (res.data) {
            this.stream.set(res.data);
            this.subscribeToStream(res.data.uuid);
          }
        },
        error: () => this.creating.set(false),
      });
  }

  endStream(): void {
    const s = this.stream();
    if (!s || this.ending()) return;
    this.ending.set(true);

    this.liveStreamService.endStream(s.uuid).subscribe({
      next: (res) => {
        this.ending.set(false);
        if (res.data) this.stream.set(res.data);
      },
      error: () => this.ending.set(false),
    });
  }

  regenerateKey(): void {
    const ch = this.selectedChannel();
    if (!ch) return;

    this.channelService.regenerateStreamKey(ch.uuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.channels.update((channels) =>
            channels.map((c) => (c.uuid === res.data!.uuid ? res.data! : c))
          );
        }
      },
    });
  }

  copyToClipboard(text: string, type: 'key' | 'url'): void {
    navigator.clipboard.writeText(text).then(() => {
      if (type === 'key') {
        this.keyCopied.set(true);
        setTimeout(() => this.keyCopied.set(false), 2000);
      } else {
        this.urlCopied.set(true);
        setTimeout(() => this.urlCopied.set(false), 2000);
      }
    });
  }

  private subscribeToStream(streamUuid: string): void {
    this.unsubscribeWs();
    const subject = this.wsService.subscribeToStream(streamUuid);
    this.wsSub = subject.subscribe((event) => {
      this.stream.update((s) => {
        if (!s || s.uuid !== event.streamUuid) return s;
        return { ...s, status: event.status, hlsPlaybackUrl: event.hlsPlaybackUrl ?? s.hlsPlaybackUrl };
      });
    });
  }

  private unsubscribeWs(): void {
    const s = this.stream();
    if (s) this.wsService.unsubscribeFromStream(s.uuid);
    this.wsSub?.unsubscribe();
    this.wsSub = null;
  }
}
