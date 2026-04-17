import axios from 'axios';
import type { LoginRequest, SignupRequest, User } from '../types/auth';

const API_BASE_URL = 'http://localhost:8080/api';

const authApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const authService = {
  signup: async (data: SignupRequest): Promise<User> => {
    const response = await authApi.post<User>('/auth/signup', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<User> => {
    const response = await authApi.post<User>('/auth/login', data);
    localStorage.setItem('user', JSON.stringify(response.data));
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('user');
  },

  getCurrentUser: (): User | null => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },
};
