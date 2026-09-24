import { Component, input } from '@angular/core';

export type AlertType = 'error' | 'success' | 'info';

/** Mensaje destacado. role="alert" hace que lectores de pantalla lo anuncien al aparecer. */
@Component({
  selector: 'app-alert',
  template: `
    <div class="alert" [class]="'alert alert-' + type()" [attr.role]="type() === 'error' ? 'alert' : 'status'">
      <ng-content />
    </div>
  `,
})
export class AlertComponent {
  readonly type = input<AlertType>('info');
}
