import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChannelService } from '../../../../core/services/channel.service';
import { Channel } from '../../../../core/models/channel.model';

@Component({
  selector: 'app-my-channels',
  imports: [RouterLink],
  templateUrl: './my-channels.html',
})
export class MyChannels implements OnInit {
  private readonly channelService = inject(ChannelService);

  protected readonly loading = signal(true);
  protected readonly channels = signal<Channel[]>([]);

  ngOnInit(): void {
    this.channelService.getMyChannels().subscribe({
      next: (res) => {
        this.channels.set(res.data ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }
}
