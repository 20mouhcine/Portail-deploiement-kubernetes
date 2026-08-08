export interface ProjectFormValue {
  owner_id: string;
  name: string;
  description: string;
  repository: string;
  allowedNamespaces: string[];
  allowedUsers: string[];

}
export default interface Project {
  id: string;
  ownerId: string | null;
  ownerUsername: string | null;
  name: string;
  description: string;
  repository: string;
  allowedNamespaces: string[];
  allowedUsers: string[];
  environmentType?: string;
  deploymentPolicy?: string;
  cpuQuota?: string;
  memoryQuota?: string;
  podQuota?: number;

}
