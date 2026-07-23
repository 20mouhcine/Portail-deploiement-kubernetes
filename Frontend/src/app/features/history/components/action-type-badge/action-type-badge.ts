import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ActionType } from '../../../../core/history/models/action-history.models';

@Component({
  selector: 'app-action-type-badge',
  templateUrl: './action-type-badge.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionTypeBadge {
  readonly action = input.required<ActionType>();

  protected readonly label = computed(() => ({
    CREATE: 'Création', UPDATE: 'Modification', DELETE: 'Suppression',
    RESTART: 'Redémarrage', SCALE: 'Mise à l’échelle', LOGIN: 'Connexion', LOGOUT: 'Déconnexion',
  })[this.action()]);

  protected readonly classes = computed(() => ({
    CREATE: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
    UPDATE: 'bg-blue-50 text-blue-700 ring-blue-600/15',
    DELETE: 'bg-red-50 text-red-700 ring-red-600/15',
    RESTART: 'bg-violet-50 text-violet-700 ring-violet-600/15',
    SCALE: 'bg-cyan-50 text-cyan-700 ring-cyan-600/15',
    LOGIN: 'bg-slate-100 text-slate-700 ring-slate-500/15',
    LOGOUT: 'bg-amber-50 text-amber-700 ring-amber-600/15',
  })[this.action()]);
}
