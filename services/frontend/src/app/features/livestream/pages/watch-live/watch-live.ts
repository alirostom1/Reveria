import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { LiveStreamService } from '../../../../core/services/livestream.service';
import { ChannelService } from '../../../../core/services/channel.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WebSocketService, ChatMessage } from '../../../../core/services/websocket.service';
import { LiveStream } from '../../../../core/models/livestream.model';
import { Channel } from '../../../../core/models/channel.model';
import { VideoPlayer } from '../../../../shared/components/video-player/video-player';

const AVATAR_COLORS = [
  'bg-coral-600', 'bg-stone-600', 'bg-amber-600', 'bg-emerald-600',
  'bg-sky-600', 'bg-violet-600', 'bg-rose-600', 'bg-teal-600',
];

function hashUuid(uuid: string): number {
  let hash = 0;
  for (let i = 0; i < uuid.length; i++) {
    hash = ((hash << 5) - hash + uuid.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

@Component({
  selector: 'app-watch-live',
  imports: [VideoPlayer, RouterLink, FormsModule],
  templateUrl: './watch-live.html',
})
export class WatchLive implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly liveStreamService = inject(LiveStreamService);
  private readonly channelService = inject(ChannelService);
  protected readonly authService = inject(AuthService);
  private readonly wsService = inject(WebSocketService);

  @ViewChild('chatScroll') chatScrollRef!: ElementRef<HTMLDivElement>;

  private wsSub: Subscription | null = null;
  private chatSub: Subscription | null = null;
  private streamUuid = '';

  protected readonly loading = signal(true);
  protected readonly stream = signal<LiveStream | null>(null);
  protected readonly channel = signal<Channel | null>(null);
  protected readonly subscribed = signal(false);
  protected readonly subscribing = signal(false);
  protected readonly liked = signal(false);
  protected readonly likeCount = signal(0);
  protected readonly linkCopied = signal(false);
  protected readonly descExpanded = signal(false);

  // Chat
  protected readonly chatMessages = signal<ChatMessage[]>([]);
  protected chatInput = '';
  protected readonly chatConnected = signal(false);

  ngOnInit(): void {
    this.streamUuid = this.route.snapshot.paramMap.get('uuid') ?? '';
    if (!this.streamUuid) {
      this.loading.set(false);
      return;
    }

    this.liveStreamService.getStream(this.streamUuid).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.stream.set(res.data);
          this.subscribeToStream(this.streamUuid);
          this.subscribeToChat(this.streamUuid);
          if (res.data.channelUuid) {
            this.loadChannel(res.data.channelUuid);
          }
        }
      },
      error: () => this.loading.set(false),
    });
  }

  ngOnDestroy(): void {
    if (this.streamUuid) {
      this.wsService.unsubscribeFromStream(this.streamUuid);
      this.wsService.unsubscribeFromChat(this.streamUuid);
    }
    this.wsSub?.unsubscribe();
    this.chatSub?.unsubscribe();
  }

  private subscribeToStream(streamUuid: string): void {
    const subject = this.wsService.subscribeToStream(streamUuid);
    this.wsSub = subject.subscribe((event) => {
      this.stream.update((s) => {
        if (!s || s.uuid !== event.streamUuid) return s;
        return { ...s, status: event.status, hlsPlaybackUrl: event.hlsPlaybackUrl ?? s.hlsPlaybackUrl };
      });
    });
  }

  private subscribeToChat(streamUuid: string): void {
    const subject = this.wsService.subscribeToChat(streamUuid);
    this.chatConnected.set(true);
    this.chatSub = subject.subscribe((msg) => {
      this.chatMessages.update((msgs) => [...msgs, msg]);
      setTimeout(() => this.scrollChatToBottom(), 0);
    });
  }

  protected sendChat(): void {
    const text = this.chatInput.trim();
    if (!text) return;
    const user = this.authService.currentUser();
    if (!user) return;

    this.wsService.sendChatMessage(this.streamUuid, {
      userUuid: user.uuid,
      displayName: user.displayName,
      avatarUrl: user.avatarUrl ?? undefined,
      content: text,
    });
    this.chatInput = '';
  }

  private scrollChatToBottom(): void {
    const el = this.chatScrollRef?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }

  private loadChannel(channelUuid: string): void {
    this.channelService.getChannel(channelUuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.channel.set(res.data);
          const user = this.authService.currentUser();
          if (user && res.data.ownerUuid !== user.uuid) {
            this.channelService.isSubscribed(res.data.uuid).subscribe({
              next: (subRes) => this.subscribed.set(subRes.data ?? false),
            });
          }
        }
      },
    });
  }

  protected toggleSubscribe(): void {
    const ch = this.channel();
    if (!ch || this.subscribing()) return;
    this.subscribing.set(true);

    const wasSub = this.subscribed();
    const action = wasSub
      ? this.channelService.unsubscribe(ch.uuid)
      : this.channelService.subscribe(ch.uuid);

    action.subscribe({
      next: () => {
        this.subscribed.set(!wasSub);
        this.channel.update((c) =>
          c ? { ...c, subscriberCount: c.subscriberCount + (wasSub ? -1 : 1) } : c,
        );
        this.subscribing.set(false);
      },
      error: () => this.subscribing.set(false),
    });
  }

  protected toggleLike(): void {
    this.liked.update((v) => !v);
    this.likeCount.update((c) => c + (this.liked() ? 1 : -1));
  }

  protected copyLink(): void {
    navigator.clipboard.writeText(window.location.href).then(() => {
      this.linkCopied.set(true);
      setTimeout(() => this.linkCopied.set(false), 2000);
    });
  }

  protected toggleDescription(): void {
    this.descExpanded.update((v) => !v);
  }

  protected isOwner(): boolean {
    const user = this.authService.currentUser();
    const ch = this.channel();
    return !!user && !!ch && ch.ownerUuid === user.uuid;
  }

  protected avatarColor(uuid: string): string {
    return AVATAR_COLORS[hashUuid(uuid) % AVATAR_COLORS.length];
  }

  protected formatCount(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
    return n.toString();
  }

  protected chatTime(timestamp: string): string {
    const d = new Date(timestamp);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
