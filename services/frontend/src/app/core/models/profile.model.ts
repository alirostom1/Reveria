export interface UserProfileResponse {
  uuid: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  emailVerified: boolean;
  hasPassword: boolean;
  createdAt: string;
  linkedProviders: string[];
}

export interface UpdateProfileRequest {
  username?: string;
  displayName?: string;
  bio?: string;
}

export interface LinkedProviderResponse {
  provider: string;
  linkedAt: string;
  canUnlink: boolean;
}
