/** Contratos de la API de identidad (/api/v1/auth y /api/v1/me). */

export interface TokenResponse {
  readonly accessToken: string;
  readonly tokenType: 'Bearer';
  readonly expiresIn: number;
  readonly expiresAt: string;
}

export interface MessageResponse {
  readonly message: string;
}

export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED';
export type Role = 'USER' | 'ADMIN';

export interface UserProfile {
  readonly id: string;
  readonly email: string;
  readonly name: string;
  readonly region: string | null;
  readonly commune: string | null;
  readonly photoUrl: string | null;
  readonly status: UserStatus;
  readonly roles: readonly Role[];
  readonly seller: boolean;
  readonly createdAt: string;
}

export interface RegisterRequest {
  readonly name: string;
  readonly email: string;
  readonly password: string;
  readonly acceptTerms: boolean;
}

export interface UpdateProfileRequest {
  readonly name?: string;
  readonly region?: string;
  readonly commune?: string;
  readonly photoUrl?: string;
}
