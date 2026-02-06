import { Component, inject } from '@angular/core';
import { ToastService, ToastType } from './toast.service';

@Component({
  selector: 'app-toast',
  imports: [],
  template: `
    <div class="fixed top-24 right-4 z-50 flex flex-col gap-3 max-w-md">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="flex items-start gap-3 rounded-2xl border px-5 py-4 text-sm font-medium shadow-2xl backdrop-blur-md"
          [class]="toastClass(toast.type)"
          [class.toast-enter]="!toast.removing"
          [class.toast-exit]="toast.removing"
        >
          <div class="shrink-0 pt-0.5" [innerHTML]="toastIcon(toast.type)"></div>
          <span class="flex-1">{{ toast.message }}</span>
        </div>
      }
    </div>
  `,
})
export class Toast {
  protected readonly toastService = inject(ToastService);

  toastClass(type: ToastType): string {
    switch (type) {
      case 'success':
        return 'border-emerald-200 bg-emerald-50/95 text-emerald-900';
      case 'error':
        return 'border-red-200 bg-red-50/95 text-red-900';
      case 'warning':
        return 'border-amber-200 bg-amber-50/95 text-amber-900';
      default:
        return 'border-sage-200 bg-sage-50/95 text-sage-900';
    }
  }

  toastIcon(type: ToastType): string {
    const cls = 'h-5 w-5';
    switch (type) {
      case 'success':
        return `<svg class="${cls} text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`;
      case 'error':
        return `<svg class="${cls} text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/></svg>`;
      case 'warning':
        return `<svg class="${cls} text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/></svg>`;
      default:
        return `<svg class="${cls} text-sage-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z"/></svg>`;
    }
  }
}
