import {
  Component,
  ElementRef,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  output,
  signal,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import Hls from 'hls.js';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.html',
})
export class VideoPlayer implements OnChanges, OnDestroy {
  @Input() src?: string;
  @Input() isPro = false;
  @Input() live = false;
  readonly playStarted = output<void>({ alias: 'playing' });
  @ViewChild('videoElement', { static: true }) videoRef!: ElementRef<HTMLVideoElement>;
  @ViewChild('progressBar') progressBarRef!: ElementRef<HTMLDivElement>;

  private hls: Hls | null = null;
  private controlsTimeout: ReturnType<typeof setTimeout> | null = null;

  protected readonly playing = signal(false);
  protected readonly loading = signal(true);
  protected readonly buffering = signal(false);
  private resumeAfterSwitch = false;
  protected readonly muted = signal(false);
  protected readonly volume = signal(1);
  protected readonly currentTime = signal(0);
  protected readonly duration = signal(0);
  protected readonly progressPercent = signal(0);
  protected readonly bufferedPercent = signal(0);
  protected readonly controlsVisible = signal(true);
  protected readonly qualityMenuOpen = signal(false);
  protected readonly qualities = signal<{ index: number; label: string; active: boolean; locked: boolean }[]>([]);
  protected readonly currentQuality = signal('Auto');

  @HostListener('document:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    const video = this.videoRef?.nativeElement;
    if (!video || !this.src) return;

    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return;

    switch (e.key) {
      case ' ':
      case 'k':
        e.preventDefault();
        this.togglePlay();
        break;
      case 'ArrowLeft':
        if (this.live) break;
        e.preventDefault();
        video.currentTime = Math.max(0, video.currentTime - 5);
        break;
      case 'ArrowRight':
        if (this.live) break;
        e.preventDefault();
        video.currentTime = Math.min(video.duration, video.currentTime + 5);
        break;
      case 'ArrowUp':
        e.preventDefault();
        this.setVolume(Math.min(1, video.volume + 0.1));
        break;
      case 'ArrowDown':
        e.preventDefault();
        this.setVolume(Math.max(0, video.volume - 0.1));
        break;
      case 'm':
        this.toggleMute();
        break;
      case 'f':
        this.toggleFullscreen();
        break;
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] && this.src) {
      this.loadSource(this.src);
    }
  }

  ngOnDestroy(): void {
    this.destroyHls();
    if (this.controlsTimeout) clearTimeout(this.controlsTimeout);
  }

  protected togglePlay(): void {
    this.qualityMenuOpen.set(false);
    const video = this.videoRef.nativeElement;
    if (video.paused) {
      video.play();
      this.playing.set(true);
      this.playStarted.emit();
    } else {
      video.pause();
      this.playing.set(false);
    }
    this.showControls();
  }

  protected toggleMute(): void {
    const video = this.videoRef.nativeElement;
    video.muted = !video.muted;
    this.muted.set(video.muted);
  }

  protected toggleFullscreen(): void {
    const container = this.videoRef.nativeElement.closest('[class*="group"]') as HTMLElement;
    if (!container) return;

    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      container.requestFullscreen();
    }
  }

  protected onVolumeChange(event: Event): void {
    const value = parseFloat((event.target as HTMLInputElement).value);
    this.setVolume(value);
  }

  protected onTimeUpdate(): void {
    const video = this.videoRef.nativeElement;
    this.currentTime.set(video.currentTime);
    if (video.duration) {
      this.progressPercent.set((video.currentTime / video.duration) * 100);
    }
    this.updateBuffered();
  }

  protected onCanPlay(): void {
    this.loading.set(false);
    this.buffering.set(false);
  }

  protected onMetadataLoaded(): void {
    this.duration.set(this.videoRef.nativeElement.duration);
  }

  protected seek(event: MouseEvent): void {
    if (this.live || !this.progressBarRef) return;
    const bar = this.progressBarRef.nativeElement;
    const rect = bar.getBoundingClientRect();
    const percent = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
    this.videoRef.nativeElement.currentTime = percent * this.videoRef.nativeElement.duration;
  }

  protected setQuality(index: number): void {
    if (!this.hls) return;
    const q = this.qualities().find((q) => q.index === index);
    if (q?.locked) return;
    const video = this.videoRef.nativeElement;
    const currentTime = video.currentTime;

    this.resumeAfterSwitch = !video.paused;
    video.pause();
    this.playing.set(false);
    this.buffering.set(true);

    this.hls.currentLevel = index;
    const label = index === -1 ? 'Auto' : this.qualities().find((q) => q.index === index)?.label || 'Auto';
    this.currentQuality.set(label);
    this.qualities.update((qs) => qs.map((q) => ({ ...q, active: q.index === index })));
    this.qualityMenuOpen.set(false);

    setTimeout(() => {
      video.currentTime = currentTime;

      const onReady = () => {
        video.removeEventListener('canplaythrough', onReady);
        this.buffering.set(false);
        if (this.resumeAfterSwitch) {
          this.resumeAfterSwitch = false;
          video.play();
          this.playing.set(true);
        }
      };
      video.addEventListener('canplaythrough', onReady);
    }, 50);
  }

  protected showControls(): void {
    this.controlsVisible.set(true);
    this.hideControlsDelayed();
  }

  protected hideControlsDelayed(): void {
    if (this.controlsTimeout) clearTimeout(this.controlsTimeout);
    this.controlsTimeout = setTimeout(() => {
      if (this.playing()) this.controlsVisible.set(false);
    }, 2500);
  }

  protected formatTime(seconds: number): string {
    if (!seconds || isNaN(seconds)) return '0:00';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private setVolume(value: number): void {
    const video = this.videoRef.nativeElement;
    video.volume = value;
    this.volume.set(value);
    if (value > 0 && video.muted) {
      video.muted = false;
      this.muted.set(false);
    }
  }

  private updateBuffered(): void {
    const video = this.videoRef.nativeElement;
    if (video.buffered.length > 0 && video.duration) {
      this.bufferedPercent.set((video.buffered.end(video.buffered.length - 1) / video.duration) * 100);
    }
  }

  private loadSource(src: string): void {
    this.destroyHls();
    this.playing.set(false);
    this.loading.set(true);
    this.currentTime.set(0);
    this.progressPercent.set(0);

    const video = this.videoRef.nativeElement;

    if (Hls.isSupported()) {
      const hlsConfig = this.live
        ? {
            liveSyncDurationCount: 3,
            liveMaxLatencyDurationCount: 5,
            liveDurationInfinity: true,
          }
        : {};
      this.hls = new Hls(hlsConfig);
      this.hls.loadSource(src);
      this.hls.attachMedia(video);

      this.hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal && this.hls) {
          if (this.live && data.type === Hls.ErrorTypes.NETWORK_ERROR) {
            this.hls.destroy();
            this.hls = null;
            setTimeout(() => this.loadSource(src), 3000);
          } else {
            this.hls.destroy();
            this.hls = null;
          }
        }
      });

      this.hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
        const levels = data.levels.map((level, i) => ({
          index: i,
          label: `${level.height}p`,
          active: false,
          locked: !this.isPro && level.height >= 2160,
        }));
        levels.unshift({ index: -1, label: 'Auto', active: true, locked: false });
        this.qualities.set(levels);
        this.currentQuality.set('Auto');
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src;
    }
  }

  private destroyHls(): void {
    if (this.hls) {
      this.hls.destroy();
      this.hls = null;
    }
    this.qualities.set([]);
  }
}
