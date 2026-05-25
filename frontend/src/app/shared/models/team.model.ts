export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface Team {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
}

export interface TeamMember {
  userId: string;
  name: string;
  email: string;
  role: TeamRole;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface AddMemberRequest {
  userId: string;
  role: TeamRole;
}
