import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { VideoService } from '../../../../core/services/video.service';
import { LiveStreamService } from '../../../../core/services/livestream.service';
import { Video } from '../../../../core/models/video.model';
import { LiveStream } from '../../../../core/models/livestream.model';

interface Category {
  id: number;
  name: string;
  slug: string;
  iconUrl?: string;
}

@Component({
  selector: 'app-explore',
  imports: [RouterLink, FormsModule],
  templateUrl: './explore.html',
})
export class Explore implements OnInit {
  private readonly videoService = inject(VideoService);
  private readonly liveStreamService = inject(LiveStreamService);

  protected readonly loading = signal(true);
  protected readonly videos = signal<Video[]>([]);
  protected readonly hasMore = signal(false);
  protected readonly totalElements = signal(0);
  protected readonly liveStreams = signal<LiveStream[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly popularTags = signal<{ name: string; slug: string }[]>([]);

  // Filters
  protected readonly selectedCategory = signal('');
  protected readonly selectedTag = signal('');
  protected readonly selectedSort = signal('latest');
  protected searchQuery = '';
  private appliedSearch = '';
  private page = 0;

  ngOnInit(): void {
    this.loadCategories();
    this.loadPopularTags();
    this.loadVideos(true);
    this.loadLiveStreams();
  }

  private loadCategories(): void {
    this.videoService.listCategories().subscribe({
      next: (res) => {
        if (res.data) this.categories.set(res.data);
      },
    });
  }

  private loadPopularTags(): void {
    this.videoService.listPopularTags().subscribe({
      next: (res) => {
        if (res.data) this.popularTags.set(res.data.map((t) => ({ name: t.name, slug: t.slug })));
      },
    });
  }

  private loadLiveStreams(): void {
    this.liveStreamService.listLiveStreams(0, 6).subscribe({
      next: (res) => {
        if (res.data) this.liveStreams.set(res.data.content);
      },
    });
  }

  protected selectCategory(slug: string): void {
    this.selectedCategory.set(slug === this.selectedCategory() ? '' : slug);
    this.resetAndLoad();
  }

  protected selectTag(slug: string): void {
    this.selectedTag.set(slug === this.selectedTag() ? '' : slug);
    this.resetAndLoad();
  }

  protected selectSort(sort: string): void {
    this.selectedSort.set(sort);
    this.resetAndLoad();
  }

  protected onSearch(): void {
    this.appliedSearch = this.searchQuery.trim();
    this.resetAndLoad();
  }

  protected clearSearch(): void {
    this.searchQuery = '';
    this.appliedSearch = '';
    this.resetAndLoad();
  }

  protected loadMore(): void {
    this.page++;
    this.loadVideos(false);
  }

  private resetAndLoad(): void {
    this.page = 0;
    this.loadVideos(true);
  }

  private loadVideos(reset: boolean): void {
    if (reset) this.loading.set(true);

    const filters: any = {};
    if (this.selectedCategory()) filters.category = this.selectedCategory();
    if (this.selectedTag()) filters.tag = this.selectedTag();
    if (this.appliedSearch) filters.search = this.appliedSearch;
    if (this.selectedSort() !== 'latest') filters.sort = this.selectedSort();

    this.videoService.listVideos(this.page, 12, filters).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          if (reset) {
            this.videos.set(res.data.content);
          } else {
            this.videos.update((prev) => [...prev, ...res.data!.content]);
          }
          this.totalElements.set(res.data.totalElements);
          this.hasMore.set(res.data.number < res.data.totalPages - 1);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  protected formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  protected formatViews(count: number): string {
    if (count >= 1_000_000) return (count / 1_000_000).toFixed(1) + 'M';
    if (count >= 1_000) return (count / 1_000).toFixed(1) + 'K';
    return count.toString();
  }

  protected timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;
    return `${Math.floor(months / 12)}y ago`;
  }
}
