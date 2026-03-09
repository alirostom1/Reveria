import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  imports: [],
  template: `
    <div class="mb-4">
      <label [for]="fieldId()" class="mb-1.5 block text-sm font-medium text-stone-700">
        {{ label() }}
      </label>
      <ng-content />
      @if (control()?.invalid && control()?.touched) {
        @if (control()?.hasError('required')) {
          <p class="mt-1.5 text-xs text-red-600">{{ label() }} is required</p>
        } @else if (control()?.hasError('email')) {
          <p class="mt-1.5 text-xs text-red-600">Please enter a valid email address</p>
        } @else if (control()?.hasError('minlength')) {
          <p class="mt-1.5 text-xs text-red-600">
            Must be at least {{ control()?.getError('minlength').requiredLength }} characters
          </p>
        } @else if (control()?.hasError('maxlength')) {
          <p class="mt-1.5 text-xs text-red-600">
            Cannot exceed {{ control()?.getError('maxlength').requiredLength }} characters
          </p>
        } @else if (control()?.hasError('pattern')) {
          <p class="mt-1.5 text-xs text-red-600">{{ patternMessage() }}</p>
        }
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
