import { Injectable, signal } from '@angular/core';

import { ActionHistory } from '../models/action-history.models';

@Injectable({ providedIn: 'root' })
export class ActionHistoryService {
  // Données temporaires : elles seront remplacées par GET /api/action-history.
  private readonly historyState = signal<readonly ActionHistory[]>([
    { id: 'history-1', action: 'RESTART', details: 'Redémarrage du déploiement après mise à jour de la configuration.', createdAt: '2026-07-23T08:42:00Z', ipAddress: '192.168.1.24', username: 'devops.demo', targetType: 'DEPLOYMENT', targetName: 'backend-api' },
    { id: 'history-2', action: 'UPDATE', details: 'Image mise à jour vers kube-portal/backend:1.4.0.', createdAt: '2026-07-23T08:37:00Z', ipAddress: '192.168.1.24', username: 'devops.demo', targetType: 'DEPLOYMENT', targetName: 'backend-api' },
    { id: 'history-3', action: 'LOGIN', details: 'Connexion réussie au portail.', createdAt: '2026-07-23T08:20:00Z', ipAddress: '192.168.1.24', username: 'devops.demo', targetType: 'SESSION', targetName: 'Session utilisateur' },
    { id: 'history-4', action: 'SCALE', details: 'Nombre de réplicas modifié de 2 à 3.', createdAt: '2026-07-22T16:15:00Z', ipAddress: '10.0.0.18', username: 'admin.demo', targetType: 'DEPLOYMENT', targetName: 'frontend-web' },
    { id: 'history-5', action: 'CREATE', details: 'Création du projet Service Notifications.', createdAt: '2026-07-22T14:05:00Z', ipAddress: '10.0.0.18', username: 'developer.demo', targetType: 'PROJECT', targetName: 'Service Notifications' },
    { id: 'history-6', action: 'UPDATE', details: 'Rôles utilisateur modifiés : DEVOPS ajouté.', createdAt: '2026-07-22T11:48:00Z', ipAddress: '10.0.0.10', username: 'admin.demo', targetType: 'USER', targetName: 'oussama' },
    { id: 'history-7', action: 'DELETE', details: 'Suppression du déploiement de test devenu obsolète.', createdAt: '2026-07-21T17:30:00Z', ipAddress: '10.0.0.10', username: 'admin.demo', targetType: 'DEPLOYMENT', targetName: 'api-test' },
    { id: 'history-8', action: 'LOGOUT', details: 'Déconnexion volontaire du portail.', createdAt: '2026-07-21T17:20:00Z', ipAddress: '192.168.1.31', username: 'developer.demo', targetType: 'SESSION', targetName: 'Session utilisateur' },
  ]);

  readonly entries = this.historyState.asReadonly();
}
