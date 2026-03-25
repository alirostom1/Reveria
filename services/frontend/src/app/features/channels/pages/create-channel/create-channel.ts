import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChannelService } from '../../../../core/services/channel.service';

@Component({
  selector: 'app-create-channel',
  imports: [FormsModule, RouterLink],
  templateUrl: './create-channel.html',
})
export class CreateChannel {
  private readonly channelService = inject(ChannelService);
  private readonly router = inject(Router);

  protected name = '';
  protected description = '';
  protected readonly submitting = signal(false);
  protected readonly error = signal('');

  onSubmit(): void {
    if (!this.name.trim()) return;

    this.submitting.set(true);
    this.error.set('');

    this.channelService.createChannel(this.name.trim(), this.description.trim() || undefined).subscribe({
      next: (res) => {
        this.submitting.set(false);
        if (res.data) {
          this.router.navigate(['/channels', res.data.uuid]);
        }
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.message || 'Failed to create channel.');
      },
    });
  }
}
