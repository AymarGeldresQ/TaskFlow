export interface Label {
  id: string;
  name: string;
  color: string;
  projectId: string;
}

export interface CreateLabelRequest {
  name: string;
  color: string;
}
