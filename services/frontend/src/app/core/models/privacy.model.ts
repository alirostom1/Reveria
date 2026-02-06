export enum ProfileVisibility {
  PUBLIC = 'PUBLIC',
  FRIENDS_ONLY = 'FRIENDS_ONLY',
  PRIVATE = 'PRIVATE',
}

export enum MessagePrivacy {
  EVERYONE = 'EVERYONE',
  FRIENDS_ONLY = 'FRIENDS_ONLY',
  NOBODY = 'NOBODY',
}

export interface PrivacySettings {
  profileVisibility: ProfileVisibility;
  showOnlineStatus: boolean;
  allowDirectMessages: boolean;
  allowFriendRequests: boolean;
  messagePrivacy: MessagePrivacy;
}
