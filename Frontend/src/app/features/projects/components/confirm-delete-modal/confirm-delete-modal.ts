import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-delete-modal',
  standalone: true,
  templateUrl: './confirm-delete-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDeleteModal {
  open = input(false);
  title = input('Confirmer la suppression');
  message = input('Êtes-vous sûr de vouloir supprimer cet élément ?');

  confirm = output<void>();
  cancel = output<void>();
}
