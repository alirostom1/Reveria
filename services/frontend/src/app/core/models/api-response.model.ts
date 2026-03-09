export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  timestamp: number;
}

export interface ApiError {
  success: false;
  message: string;
  errors?: Record<string, string>;
  timestamp: number;
}
