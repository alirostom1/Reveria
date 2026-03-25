import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { VideoService } from '../../../../core/services/video.service';
import { ChannelService } from '../../../../core/services/channel.service';
import { WebSocketService, VideoProgressEvent } from '../../../../core/services/websocket.service';
import { Channel } from '../../../../core/models/channel.model';

type Step = 'form' | 'uploading' | 'processing' | 'done' | 'error';

@Component({
  selector: 'app-upload-video',
  imports: [FormsModule, RouterLink],
  templateUrl: './upload-video.html',
})
export class UploadVideo implements OnInit, OnDestroy {
  private readonly videoService = inject(VideoService);
  private readonly channelService = inject(ChannelService);
  private readonly wsService = inject(WebSocketService);
  private readonly router = inject(Router);

  private wsSub: Subscription | null = null;
  private videoUuid = '';

  // Form
  protected readonly channels = signal<Channel[]>([]);
  protected readonly categories = signal<{ id: number; name: string; slug: string }[]>([]);
  protected readonly loadingChannels = signal(true);
  protected selectedChannelUuid = '';
  protected videoTitle = '';
  protected description = '';
  protected selectedCategorySlug = '';
  protected tagInput = '';
  protected readonly tags = signal<string[]>([]);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly error = signal('');

  // Channel creation
  protected readonly showCreateChannel = signal(false);
  protected newChannelName = '';

  // Progress
  protected readonly step = signal<Step>('form');
  protected readonly uploadPercent = signal(0);
  protected readonly processProgress = signal(0);
  protected readonly processMessage = signal('Waiting for processing...');
  protected readonly processStep = signal('');

  protected readonly stepIndex = computed(() => {
    const s = this.step();
    if (s === 'form') return 0;
    if (s === 'uploading') return 1;
    if (s === 'processing') return 2;
    if (s === 'done') return 3;
    return 0;
  });

  private readonly STEPS = [
    { key: 'PROBING', label: 'Analyzing video' },
    { key: 'THUMBNAIL', label: 'Generating thumbnail' },
    { key: 'TRANSCODING', label: 'Encoding qualities' },
    { key: 'UPLOADING', label: 'Uploading to storage' },
    { key: 'MP4', label: 'Creating downloads' },
  ];

  private readonly STEP_ORDER = this.STEPS.map((s) => s.key);

  protected readonly processingSteps = computed(() => {
    const current = this.processStep();
    const currentIdx = this.STEP_ORDER.indexOf(current);
    return this.STEPS.map((s, i) => ({
      ...s,
      done: currentIdx > i || this.step() === 'done',
      active: currentIdx === i && this.step() === 'processing',
    }));
  });

  ngOnInit(): void {
    this.loadChannels();
    this.loadCategories();
  }

  ngOnDestroy(): void {
    if (this.videoUuid) {
      this.wsService.unsubscribeFromVideoProgress(this.videoUuid);
    }
    this.wsSub?.unsubscribe();
  }

  private loadChannels(): void {
    this.loadingChannels.set(true);
    this.channelService.getMyChannels().subscribe({
      next: (res) => {
        const list = res.data ?? [];
        this.channels.set(list);
        if (list.length === 1) this.selectedChannelUuid = list[0].uuid;
        this.loadingChannels.set(false);
      },
      error: () => {
        this.channels.set([]);
        this.loadingChannels.set(false);
      },
    });
  }

  private loadCategories(): void {
    this.videoService.listCategories().subscribe({
      next: (res) => {
        if (res.data) this.categories.set(res.data);
      },
    });
  }

  protected addTag(): void {
    const tag = this.tagInput.trim();
    if (tag && !this.tags().includes(tag) && this.tags().length < 10) {
      this.tags.update((t) => [...t, tag]);
      this.tagInput = '';
    }
  }

  protected removeTag(tag: string): void {
    this.tags.update((t) => t.filter((x) => x !== tag));
  }

  protected onTagKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addTag();
    }
  }

  protected createChannel(): void {
    if (!this.newChannelName.trim()) return;
    this.channelService.createChannel(this.newChannelName.trim()).subscribe({
      next: (res) => {
        if (res.data) {
          this.channels.update((ch) => [...ch, res.data!]);
          this.selectedChannelUuid = res.data.uuid;
          this.newChannelName = '';
          this.showCreateChannel.set(false);
        }
      },
      error: (err) => this.error.set(err.error?.message || 'Failed to create channel.'),
    });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.selectedFile.set(input.files[0]);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file && file.type.startsWith('video/')) this.selectedFile.set(file);
  }

  protected clearFile(): void {
    this.selectedFile.set(null);
  }

  protected formatFileSize(bytes: number): string {
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  protected onSubmit(): void {
    const file = this.selectedFile();
    if (!this.videoTitle.trim() || !file || !this.selectedChannelUuid) return;

    this.step.set('uploading');
    this.uploadPercent.set(0);
    this.error.set('');

    this.videoService
      .uploadVideoWithProgress(
        this.selectedChannelUuid, this.videoTitle.trim(), this.description.trim(), file,
        this.selectedCategorySlug || undefined, this.tags().length ? this.tags() : undefined
      )
      .subscribe((event) => {
        if (event.phase === 'uploading') {
          this.uploadPercent.set(event.percent);
        } else if (event.phase === 'done' && event.video) {
          this.videoUuid = event.video.uuid;
          this.step.set('processing');
          this.processProgress.set(0);
          this.processMessage.set('Queued for processing...');
          this.subscribeToProgress(event.video.uuid);
        } else if (event.phase === 'error') {
          this.step.set('error');
          this.error.set(event.error || 'Upload failed');
        }
      });
  }

  private subscribeToProgress(videoUuid: string): void {
    const subject = this.wsService.subscribeToVideoProgress(videoUuid);
    this.wsSub = subject.subscribe((event: VideoProgressEvent) => {
      this.processProgress.set(event.progress);
      this.processMessage.set(event.message);
      this.processStep.set(event.step);

      if (event.status === 'READY') {
        this.step.set('done');
      } else if (event.status === 'FAILED') {
        this.step.set('error');
        this.error.set(event.message || 'Processing failed');
      }
    });
  }

  protected goToVideo(): void {
    this.router.navigate(['/watch', this.videoUuid]);
  }

  protected retry(): void {
    this.step.set('form');
    this.uploadPercent.set(0);
    this.processProgress.set(0);
    this.error.set('');
    this.videoUuid = '';
  }
}
