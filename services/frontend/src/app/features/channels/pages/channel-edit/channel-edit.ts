import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ChannelService } from '../../../../core/services/channel.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Channel } from '../../../../core/models/channel.model';

@Component({
  selector: 'app-channel-edit',
  imports: [FormsModule],
  templateUrl: './channel-edit.html',
})
export class ChannelEdit implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly channelService = inject(ChannelService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly channel = signal<Channel | null>(null);
  protected readonly avatarPreview = signal<string | null>(null);
  protected readonly bannerPreview = signal<string | null>(null);

  protected name = '';
  protected description = '';
  protected channelUuid = '';

  ngOnInit(): void {
    this.channelUuid = this.route.snapshot.params['uuid'];
    this.channelService.getChannel(this.channelUuid).subscribe({
      next: (res) => {
        if (res.data) {
          this.channel.set(res.data);
          this.name = res.data.name;
          this.description = res.data.description ?? '';
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Channel not found.');
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => this.avatarPreview.set(reader.result as string);
    reader.readAsDataURL(file);

    this.channelService.uploadAvatar(this.channelUuid, file).subscribe({
      next: (res) => {
        if (res.data) this.channel.set(res.data);
        this.toast.success('Avatar updated!');
      },
      error: () => {
        this.avatarPreview.set(null);
        this.toast.error('Failed to upload avatar.');
      },
    });
  }

  removeAvatar(): void {
    this.channelService.updateChannel(this.channelUuid, { avatarUrl: '' }).subscribe({
      next: (res) => {
        if (res.data) this.channel.set(res.data);
        this.avatarPreview.set(null);
        this.toast.success('Avatar removed.');
      },
      error: () => this.toast.error('Failed to remove avatar.'),
    });
  }

  onBannerSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => this.bannerPreview.set(reader.result as string);
    reader.readAsDataURL(file);

    this.channelService.uploadBanner(this.channelUuid, file).subscribe({
      next: (res) => {
        if (res.data) this.channel.set(res.data);
        this.toast.success('Banner updated!');
      },
      error: () => {
        this.bannerPreview.set(null);
        this.toast.error('Failed to upload banner.');
      },
    });
  }

  removeBanner(): void {
    this.channelService.updateChannel(this.channelUuid, { bannerUrl: '' }).subscribe({
      next: (res) => {
        if (res.data) this.channel.set(res.data);
        this.bannerPreview.set(null);
        this.toast.success('Banner removed.');
      },
      error: () => this.toast.error('Failed to remove banner.'),
    });
  }

  onSubmit(): void {
    if (!this.name.trim()) return;

    this.saving.set(true);

    this.channelService
      .updateChannel(this.channelUuid, {
        name: this.name.trim(),
        description: this.description.trim() || undefined,
      })
      .subscribe({
        next: (res) => {
          this.saving.set(false);
          if (res.data) this.channel.set(res.data);
          this.toast.success('Channel updated!');
        },
        error: (err) => {
          this.saving.set(false);
          this.toast.error(err.error?.message || 'Failed to update channel.');
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/channels', this.channelUuid]);
  }
}
