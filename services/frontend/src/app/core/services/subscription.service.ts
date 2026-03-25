import { Injectable, signal } from '@angular/core';

// TODO: Replace with real subscription-service API calls when that service is built
@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  readonly isPro = signal(false);

  checkSubscription(_userUuid: string): void {
    // Stub: always Free for now
    this.isPro.set(false);
  }
}
