import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-status-card',
  templateUrl: './status-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly description = input.required<string>();
  readonly tone = input<'blue' | 'emerald' | 'amber'>('blue');
}
