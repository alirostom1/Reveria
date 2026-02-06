import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  imports: [],
  template: `
    <div class="mb-5">
      <label [for]="fieldId()" class="mb-2 block text-sm font-semibold text-stone-700">
        {{ label() }}
      </label>
      <ng-content />
      @if (control()?.invalid && control()?.touched) {
        <div class="mt-2 flex items-start gap-2">
          <svg class="h-4 w-4 shrink-0 text-red-500 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
          </svg>
          @if (control()?.hasError('required')) {
            <p class="text-sm text-red-600 font-medium">{{ label() }} is required</p>
          } @else if (control()?.hasError('email')) {
            <p class="text-sm text-red-600 font-medium">Please enter a valid email address</p>
          } @else if (control()?.hasError('minlength')) {
            <p class="text-sm text-red-600 font-medium">
              Must be at least {{ control()?.getError('minlength').requiredLength }} characters
            </p>
          } @else if (control()?.hasError('maxlength')) {
            <p class="text-sm text-red-600 font-medium">
              Cannot exceed {{ control()?.getError('maxlength').requiredLength }} characters
            </p>
          } @else if (control()?.hasError('pattern')) {
            <p class="text-sm text-red-600 font-medium">{{ patternMessage() }}</p>
          }
        </div>
      }
    </div>
  `,
})
export class FormField {
  readonly label = input.required<string>();
  readonly fieldId = input.required<string>();
  readonly control = input<AbstractControl | null>(null);
  readonly patternMessage = input<string>('Invalid format');
}
