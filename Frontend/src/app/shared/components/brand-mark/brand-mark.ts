import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-brand-mark',
  templateUrl: './brand-mark.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandMark {
  readonly compact = input(false);
}
