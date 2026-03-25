import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { VideoService } from '../../../../core/services/video.service';
import { ChannelService } from '../../../../core/services/channel.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SubscriptionService } from '../../../../core/services/subscription.service';
import { VideoPlayer } from '../../../../shared/components/video-player/video-player';
import { Video, Rendition } from '../../../../core/models/video.model';
import { Channel } from '../../../../core/models/channel.model';
import { Comment } from '../../../../core/models/comment.model';

// Consistent avatar colors derived from UUID
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
  selector: 'app-watch',
  imports: [VideoPlayer, RouterLink, FormsModule],
  templateUrl: './watch.html',
})
export class Watch implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly videoService = inject(VideoService);
  private readonly channelService = inject(ChannelService);
  protected readonly authService = inject(AuthService);
  protected readonly subscriptionService = inject(SubscriptionService);

  protected readonly loading = signal(true);
  protected readonly video = signal<Video | null>(null);
  protected readonly channel = signal<Channel | null>(null);
  protected readonly liked = signal(false);
  protected readonly liking = signal(false);
  protected readonly subscribed = signal(false);
  protected readonly subscribing = signal(false);
  protected readonly isOwner = signal(false);

  // Description
  protected readonly descExpanded = signal(false);

  // Share
  protected readonly linkCopied = signal(false);

  // Download
  protected readonly downloading = signal(false);
  protected readonly downloadMenuOpen = signal(false);

  // Comments
  protected readonly comments = signal<Comment[]>([]);
  protected readonly commentsLoading = signal(false);
  protected readonly hasMoreComments = signal(false);
  protected readonly submittingComment = signal(false);
  protected readonly commentFocused = signal(false);
  protected commentText = '';
  private commentPage = 0;

  // Replies state
  protected readonly replyState = signal<Record<number, ReplyState>>({});

  // Similar videos
  protected readonly similarVideos = signal<Video[]>([]);

  private videoUuid = '';
  private viewRecorded = false;
  private routeSub?: Subscription;

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const uuid = params.get('uuid') ?? '';
      if (uuid && uuid !== this.videoUuid) {
        this.videoUuid = uuid;
        this.loadVideo();
      } else if (!uuid) {
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private loadVideo(): void {
    this.loading.set(true);
    this.viewRecorded = false;
    this.video.set(null);
    this.channel.set(null);
    this.comments.set([]);
    this.similarVideos.set([]);
    this.commentPage = 0;
    this.replyState.set({});
    this.liked.set(false);
    this.subscribed.set(false);
    this.descExpanded.set(false);

    this.videoService.getVideo(this.videoUuid).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.video.set(res.data);
          this.loadChannel(res.data);
          this.loadInteractions();
          this.loadComments();
          this.loadSimilarVideos();
          const user = this.authService.currentUser();
          if (user) this.subscriptionService.checkSubscription(user.uuid);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  // --- Likes ---

  toggleLike(): void {
    if (this.liking()) return;
    this.liking.set(true);

    const wasLiked = this.liked();
    const action = wasLiked
      ? this.videoService.unlikeVideo(this.videoUuid)
      : this.videoService.likeVideo(this.videoUuid);

    action.subscribe({
      next: () => {
        this.liked.set(!wasLiked);
        this.video.update((v) =>
          v ? { ...v, likeCount: (v.likeCount ?? 0) + (wasLiked ? -1 : 1) } : v,
        );
        this.liking.set(false);
      },
      error: () => this.liking.set(false),
    });
  }

  // --- Subscribe ---

  toggleSubscribe(): void {
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

  // --- Views ---

  onVideoPlaying(): void {
    if (this.viewRecorded) return;
    this.viewRecorded = true;
    this.videoService.recordView(this.videoUuid).subscribe();
    this.video.update((v) => (v ? { ...v, viewCount: (v.viewCount ?? 0) + 1 } : v));
  }

  // --- Share ---

  copyLink(): void {
    navigator.clipboard.writeText(window.location.href).then(() => {
      this.linkCopied.set(true);
      setTimeout(() => this.linkCopied.set(false), 2000);
    });
  }

  // --- Description ---

  toggleDescription(): void {
    this.descExpanded.update((v) => !v);
  }

  // --- Download ---

  downloadVideo(quality: string): void {
    if (this.downloading()) return;
    this.downloading.set(true);
    this.downloadMenuOpen.set(false);

    this.videoService.requestDownload(this.videoUuid, quality).subscribe({
      next: (res) => {
        this.downloading.set(false);
        if (res.data) {
          window.open(this.videoService.getDownloadUrl(res.data.downloadToken), '_blank');
        }
      },
      error: () => this.downloading.set(false),
    });
  }

  protected readableQualities(): Rendition[] {
    return (this.video()?.renditions ?? []).filter((r) => r.status === 'READY' && r.mp4FileUrl);
  }

  protected formatFileSize(bytes?: number): string {
    if (!bytes) return '';
    if (bytes >= 1_073_741_824) return (bytes / 1_073_741_824).toFixed(1) + ' GB';
    if (bytes >= 1_048_576) return (bytes / 1_048_576).toFixed(0) + ' MB';
    return (bytes / 1024).toFixed(0) + ' KB';
  }

  // --- Comments ---

  submitComment(): void {
    if (!this.commentText.trim() || this.submittingComment()) return;
    this.submittingComment.set(true);

    this.videoService.addComment(this.videoUuid, this.commentText.trim()).subscribe({
      next: (res) => {
        if (res.data) {
          this.comments.update((c) => [res.data!, ...c]);
          this.video.update((v) => (v ? { ...v, commentCount: (v.commentCount ?? 0) + 1 } : v));
        }
        this.commentText = '';
        this.commentFocused.set(false);
        this.submittingComment.set(false);
      },
      error: () => this.submittingComment.set(false),
    });
  }

  cancelComment(): void {
    this.commentText = '';
    this.commentFocused.set(false);
  }

  loadMoreComments(): void {
    this.commentPage++;
    this.loadComments();
  }

  deleteComment(commentId: number): void {
    this.videoService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments.update((c) => c.filter((x) => x.id !== commentId));
        this.video.update((v) => (v ? { ...v, commentCount: Math.max(0, (v.commentCount ?? 1) - 1) } : v));
      },
    });
  }

  // --- Replies ---

  toggleReplies(commentId: number): void {
    const state = this.replyState();
    const current = state[commentId];

    if (current?.expanded) {
      this.replyState.update((s) => ({ ...s, [commentId]: { ...current, expanded: false } }));
      return;
    }

    if (current?.replies.length) {
      this.replyState.update((s) => ({ ...s, [commentId]: { ...current, expanded: true } }));
      return;
    }

    const newState: ReplyState = { replies: [], expanded: true, loading: true, hasMore: false, page: 0, replyText: '', submitting: false };
    this.replyState.update((s) => ({ ...s, [commentId]: newState }));

    this.videoService.getReplies(commentId, 0).subscribe({
      next: (res) => {
        if (res.data) {
          this.replyState.update((s) => ({
            ...s,
            [commentId]: {
              ...s[commentId],
              replies: res.data!.content,
              loading: false,
              hasMore: res.data!.number < res.data!.totalPages - 1,
            },
          }));
        }
      },
    });
  }

  loadMoreReplies(commentId: number): void {
    const state = this.replyState()[commentId];
    if (!state || !state.hasMore || state.loading) return;

    const nextPage = state.page + 1;
    this.replyState.update((s) => ({ ...s, [commentId]: { ...s[commentId], loading: true, page: nextPage } }));

    this.videoService.getReplies(commentId, nextPage).subscribe({
      next: (res) => {
        if (res.data) {
          this.replyState.update((s) => ({
            ...s,
            [commentId]: {
              ...s[commentId],
              replies: [...s[commentId].replies, ...res.data!.content],
              loading: false,
              hasMore: res.data!.number < res.data!.totalPages - 1,
            },
          }));
        }
      },
    });
  }

  submitReply(commentId: number): void {
    const state = this.replyState()[commentId];
    if (!state || !state.replyText.trim() || state.submitting) return;

    this.replyState.update((s) => ({ ...s, [commentId]: { ...state, submitting: true } }));

    this.videoService.addReply(commentId, state.replyText.trim()).subscribe({
      next: (res) => {
        if (res.data) {
          this.replyState.update((s) => ({
            ...s,
            [commentId]: {
              ...s[commentId],
              replies: [...s[commentId].replies, res.data!],
              replyText: '',
              submitting: false,
            },
          }));
        }
      },
      error: () => {
        this.replyState.update((s) => ({ ...s, [commentId]: { ...s[commentId], submitting: false } }));
      },
    });
  }

  updateReplyText(commentId: number, text: string): void {
    this.replyState.update((s) => ({ ...s, [commentId]: { ...s[commentId], replyText: text } }));
  }

  // --- Helpers ---

  protected avatarColor(uuid: string): string {
    return AVATAR_COLORS[hashUuid(uuid) % AVATAR_COLORS.length];
  }

  protected avatarInitial(uuid: string): string {
    return uuid.charAt(0).toUpperCase();
  }

  protected isCommentOwner(comment: Comment): boolean {
    return comment.userUuid === this.authService.currentUser()?.uuid;
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  protected timeAgo(dateStr: string): string {
    const seconds = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;
    return this.formatDate(dateStr);
  }

  protected formatCount(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
    return n.toString();
  }

  protected formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private loadChannel(video: Video): void {
    if (!video.channelUuid) return;
    this.channelService.getChannel(video.channelUuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.channel.set(res.data);
          const user = this.authService.currentUser();
          if (user) {
            this.isOwner.set(res.data.ownerUuid === user.uuid);
            if (!this.isOwner()) {
              this.channelService.isSubscribed(res.data.uuid).subscribe({
                next: (subRes) => this.subscribed.set(subRes.data ?? false),
              });
            }
          }
        }
      },
    });
  }

  private loadInteractions(): void {
    this.videoService.hasLiked(this.videoUuid).subscribe({
      next: (res) => this.liked.set(res.data ?? false),
    });
  }

  private loadComments(): void {
    this.commentsLoading.set(true);
    this.videoService.getComments(this.videoUuid, this.commentPage).subscribe({
      next: (res) => {
        this.commentsLoading.set(false);
        if (res.data) {
          this.comments.update((prev) => [...prev, ...res.data!.content]);
          this.hasMoreComments.set(res.data.number < res.data.totalPages - 1);
        }
      },
      error: () => this.commentsLoading.set(false),
    });
  }

  private loadSimilarVideos(): void {
    this.videoService.listVideos(0, 12).subscribe({
      next: (res) => {
        if (res.data) {
          this.similarVideos.set(res.data.content.filter((v) => v.uuid !== this.videoUuid).slice(0, 8));
        }
      },
    });
  }
}

interface ReplyState {
  replies: Comment[];
  expanded: boolean;
  loading: boolean;
  hasMore: boolean;
  page: number;
  replyText: string;
  submitting: boolean;
}
