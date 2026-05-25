export interface Project {
  id: string;
  name: string;
  description: string | null;
  teamId: string;
  createdAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  teamId: string;
}
