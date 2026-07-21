export interface ProjectFormValue {
  owner_id: number;
  name: string;
  description: string;
  repository: string;
}
export default interface Project {
  id: number;
  owner_id: number;
  name: string;
  description: string;
  repository: string;
}
