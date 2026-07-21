export interface ProjectFormValue {
  owner_id: string;
  name: string;
  description: string;
  repository: string;
}
export default interface Project {
  id: string;
  ownerId: string | null;
  ownerUsername: string | null;
  name: string;
  description: string;
  repository: string;
}
