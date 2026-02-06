export interface UserProfileResponse {
  uuid: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  emailVerified: boolean;
  createdAt: string;
  linkedProviders: string[];
}

export interface UpdateProfileRequest {
  username?: string;
  displayName?: string;
  bio?: string;
}
