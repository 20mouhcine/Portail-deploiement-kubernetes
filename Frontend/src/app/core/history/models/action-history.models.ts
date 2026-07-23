export type ActionType = 'CREATE' | 'UPDATE' | 'DELETE' | 'RESTART' | 'SCALE' | 'LOGIN' | 'LOGOUT';

export interface ActionHistory {
  readonly id: string;
  readonly action: ActionType;
  readonly details: string;
  readonly createdAt: string;
  readonly ipAddress: string;
  readonly username: string;
  readonly targetType: 'PROJECT' | 'DEPLOYMENT' | 'USER' | 'SESSION';
  readonly targetName: string;
}
