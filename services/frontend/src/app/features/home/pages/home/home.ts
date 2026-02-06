import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <div class="relative flex min-h-[calc(100vh-72px)] items-center justify-center overflow-hidden px-4">
      <!-- Background decorative elements -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden">
        <div
          class="animate-float absolute -top-40 -left-40 h-[500px] w-[500px] rounded-full bg-coral-200/40 blur-3xl"
        ></div>
        <div
          class="animate-float-delay-1 absolute -right-40 top-20 h-[400px] w-[400px] rounded-full bg-sage-200/40 blur-3xl"
        ></div>
        <div
          class="animate-float-delay-2 absolute -bottom-40 left-1/3 h-[450px] w-[450px] rounded-full bg-sand-300/30 blur-3xl"
        ></div>
      </div>

      <!-- Content -->
      <div class="animate-fade-up relative z-10 mx-auto max-w-4xl text-center">
        <div class="mb-8 inline-flex items-center gap-2.5 rounded-full border-2 border-coral-200 bg-coral-50/80 px-5 py-2 text-sm font-semibold text-coral-700 backdrop-blur-sm">
          <svg class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
          </svg>
          Your creative universe awaits
        </div>

        <h1 class="text-gradient mb-8 text-6xl leading-tight font-extrabold tracking-tight sm:text-7xl lg:text-8xl">
          Where imagination<br />becomes reality
        </h1>

        <p class="mx-auto mb-12 max-w-2xl text-xl leading-relaxed text-stone-600 sm:text-2xl font-light">
          Discover, create, and share worlds with a community of storytellers, artists, and dreamers.
        </p>

        @if (!authService.isAuthenticated()) {
          <div class="flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a
              routerLink="/auth/register"
              class="btn-shine group inline-flex items-center gap-2.5 rounded-2xl bg-gradient-to-r from-coral-500 to-coral-600 px-9 py-4 text-base font-bold text-white shadow-2xl shadow-coral-500/30 transition-all duration-200 hover:shadow-coral-500/40 hover:-translate-y-1 hover:scale-105"
            >
              Start creating
              <svg class="h-5 w-5 transition-transform duration-200 group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
              </svg>
            </a>
            <a
              routerLink="/auth/login"
              class="inline-flex items-center gap-2.5 rounded-2xl border-2 border-stone-300 bg-white/90 px-9 py-4 text-base font-bold text-stone-800 shadow-lg backdrop-blur-sm transition-all duration-200 hover:border-stone-400 hover:shadow-xl hover:-translate-y-1"
            >
              Log in
            </a>
          </div>
        } @else {
          <div class="flex items-center justify-center">
            <a
              routerLink="/profile"
              class="btn-shine group inline-flex items-center gap-2.5 rounded-2xl bg-gradient-to-r from-coral-500 to-coral-600 px-9 py-4 text-base font-bold text-white shadow-2xl shadow-coral-500/30 transition-all duration-200 hover:shadow-coral-500/40 hover:-translate-y-1 hover:scale-105"
            >
              Go to your profile
              <svg class="h-5 w-5 transition-transform duration-200 group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
              </svg>
            </a>
          </div>
        }
      </div>
    </div>
  `,
})
export class Home {
  protected readonly authService = inject(AuthService);
}
