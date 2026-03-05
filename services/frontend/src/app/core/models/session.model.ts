export interface SessionResponse {
  familyId: string;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
  currentSession: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string | null;
  newPassword: string;
  revokeOtherSessions: boolean;
}

