export interface User {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  status: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface Address {
  id: number;
  label: string | null;
  fullName: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string | null;
  postalCode: string;
  country: string;
  phone: string;
  isDefault: boolean;
}

export type AddressRequest = Omit<Address, 'id'>;
