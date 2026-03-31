import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SocialService } from '../../../../core/services/social.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Post, PostComment, Story, PublicUser } from '../../../../core/models/social.model';

@Component({
  selector: 'app-feed',
  imports: [FormsModule, RouterLink],
  templateUrl: './feed.html',
})
export class Feed implements OnInit {
  private readonly socialService = inject(SocialService);
  protected readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly posts = signal<Post[]>([]);
  protected readonly hasMore = signal(false);
  protected readonly stories = signal<Story[]>([]);
  protected readonly storiesLoading = signal(true);

  // Create post
  protected readonly showCreatePost = signal(false);
  protected readonly creating = signal(false);
  protected postContent = '';
  protected postVisibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE' = 'PUBLIC';
  protected selectedFiles: File[] = [];
  protected readonly previewUrls = signal<string[]>([]);

  // Profile cache
  protected readonly profiles = signal<Map<string, PublicUser>>(new Map());

  // Edit / delete
  protected readonly editingPostUuid = signal<string | null>(null);
  protected readonly editingSaving = signal(false);
  protected editContent = '';
  protected editVisibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE' = 'PUBLIC';

  // Comments
  protected readonly expandedComments = signal<Set<string>>(new Set());
  protected readonly postComments = signal<Map<string, PostComment[]>>(new Map());
  protected readonly commentInputs = signal<Map<string, string>>(new Map());
  protected readonly replyInputs = signal<Map<number, string>>(new Map());
  protected readonly showReplies = signal<Set<number>>(new Set());

  private page = 0;

  ngOnInit(): void {
    this.loadFeed(true);
    this.loadStories();
  }

  private loadFeed(reset: boolean): void {
    if (reset) this.loading.set(true);

    this.socialService.getFeed(this.page, 20).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          if (reset) {
            this.posts.set(res.data.content);
          } else {
            this.posts.update((prev) => [...prev, ...res.data!.content]);
          }
          this.hasMore.set(res.data.number < res.data.totalPages - 1);
          this.resolveProfiles(res.data.content.map((p) => p.authorUuid));
        }
      },
      error: () => this.loading.set(false),
    });
  }

  private loadStories(): void {
    this.socialService.getFeedStories().subscribe({
      next: (res) => {
        this.storiesLoading.set(false);
        if (res.data) {
          this.stories.set(res.data);
          this.resolveProfiles(res.data.map((s) => s.authorUuid));
        }
      },
      error: () => this.storiesLoading.set(false),
    });
  }

  protected loadMore(): void {
    this.page++;
    this.loadFeed(false);
  }

  // ── Create Post ──

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    this.selectedFiles = Array.from(input.files);
    this.previewUrls().forEach((url) => URL.revokeObjectURL(url));
    this.previewUrls.set(this.selectedFiles.map((f) => URL.createObjectURL(f)));
  }

  protected removeFile(index: number): void {
    URL.revokeObjectURL(this.previewUrls()[index]);
    this.selectedFiles.splice(index, 1);
    this.previewUrls.update((urls) => urls.filter((_, i) => i !== index));
  }

  protected submitPost(): void {
    const content = this.postContent.trim();
    if (!content && this.selectedFiles.length === 0) return;

    this.creating.set(true);
    const type = this.selectedFiles.length > 0 ? 'IMAGE' : 'TEXT';

    this.socialService
      .createPost(
        { content: content || undefined, type, visibility: this.postVisibility },
        this.selectedFiles.length > 0 ? this.selectedFiles : undefined,
      )
      .subscribe({
        next: (res) => {
          this.creating.set(false);
          if (res.data) {
            this.posts.update((prev) => [res.data!, ...prev]);
            this.postContent = '';
            this.selectedFiles = [];
            this.previewUrls().forEach((url) => URL.revokeObjectURL(url));
            this.previewUrls.set([]);
            this.showCreatePost.set(false);
            this.toast.success('Post created');
          }
        },
        error: () => {
          this.creating.set(false);
          this.toast.error('Failed to create post');
        },
      });
  }

  // ── Likes ──

  protected toggleLike(post: Post): void {
    this.socialService.toggleLike(post.uuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.posts.update((posts) =>
            posts.map((p) => (p.uuid === post.uuid ? res.data! : p)),
          );
        }
      },
    });
  }

  // ── Comments ──

  protected toggleComments(uuid: string): void {
    this.expandedComments.update((set) => {
      const next = new Set(set);
      if (next.has(uuid)) {
        next.delete(uuid);
      } else {
        next.add(uuid);
        if (!this.postComments().has(uuid)) this.loadComments(uuid);
      }
      return next;
    });
  }

  private loadComments(uuid: string): void {
    this.socialService.getComments(uuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.postComments.update((map) => new Map(map).set(uuid, res.data!.content));
          const uuids = res.data.content.flatMap((c) => [
            c.userUuid,
            ...c.replies.map((r) => r.userUuid),
          ]);
          this.resolveProfiles(uuids);
        }
      },
    });
  }

  protected getCommentInput(uuid: string): string {
    return this.commentInputs().get(uuid) ?? '';
  }

  protected setCommentInput(uuid: string, value: string): void {
    this.commentInputs.update((map) => new Map(map).set(uuid, value));
  }

  protected submitComment(uuid: string): void {
    const content = this.getCommentInput(uuid).trim();
    if (!content) return;

    this.socialService.addComment(uuid, content).subscribe({
      next: (res) => {
        if (res.data) {
          this.postComments.update((map) => {
            const next = new Map(map);
            const existing = next.get(uuid) ?? [];
            next.set(uuid, [res.data!, ...existing]);
            return next;
          });
          this.commentInputs.update((map) => new Map(map).set(uuid, ''));
          this.posts.update((posts) =>
            posts.map((p) => (p.uuid === uuid ? { ...p, commentCount: p.commentCount + 1 } : p)),
          );
        }
      },
    });
  }

  protected toggleReplies(commentId: number): void {
    this.showReplies.update((set) => {
      const next = new Set(set);
      if (next.has(commentId)) next.delete(commentId);
      else next.add(commentId);
      return next;
    });
  }

  protected getReplyInput(commentId: number): string {
    return this.replyInputs().get(commentId) ?? '';
  }

  protected setReplyInput(commentId: number, value: string): void {
    this.replyInputs.update((map) => new Map(map).set(commentId, value));
  }

  protected submitReply(uuid: string, commentId: number): void {
    const content = this.getReplyInput(commentId).trim();
    if (!content) return;

    this.socialService.addReply(commentId, content).subscribe({
      next: (res) => {
        if (res.data) {
          this.postComments.update((map) => {
            const next = new Map(map);
            const comments = (next.get(uuid) ?? []).map((c) =>
              c.id === commentId ? { ...c, replies: [...c.replies, res.data!] } : c,
            );
            next.set(uuid, comments);
            return next;
          });
          this.replyInputs.update((map) => new Map(map).set(commentId, ''));
        }
      },
    });
  }

  protected startEdit(post: Post): void {
    this.editingPostUuid.set(post.uuid);
    this.editContent = post.content ?? '';
    this.editVisibility = post.visibility;
  }

  protected cancelEdit(): void {
    this.editingPostUuid.set(null);
  }

  protected saveEdit(uuid: string): void {
    const content = this.editContent.trim();
    if (!content) return;
    this.editingSaving.set(true);

    this.socialService.updatePost(uuid, content, this.editVisibility).subscribe({
      next: (res) => {
        this.editingSaving.set(false);
        if (res.data) {
          this.posts.update((posts) => posts.map((p) => (p.uuid === uuid ? res.data! : p)));
          this.editingPostUuid.set(null);
          this.toast.success('Post updated');
        }
      },
      error: () => {
        this.editingSaving.set(false);
        this.toast.error('Failed to update post');
      },
    });
  }

  protected deletePost(uuid: string): void {
    this.socialService.deletePost(uuid).subscribe({
      next: () => {
        this.posts.update((posts) => posts.filter((p) => p.uuid !== uuid));
        this.toast.success('Post deleted');
      },
    });
  }

  // ── Stories ──

  protected onStoryFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.socialService.createStory(file).subscribe({
      next: (res) => {
        if (res.data) {
          this.stories.update((s) => [res.data!, ...s]);
          this.toast.success('Story created');
        }
      },
      error: () => this.toast.error('Failed to create story'),
    });
    input.value = '';
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
