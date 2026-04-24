import axios from 'axios';
import type { LoginRequest, SignupRequest, User } from '../types/auth';

const API_BASE_URL = 'http://localhost:8080/api';

// 인증 전용 API (로그인, 회원가입용)
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
    
    // [JWT 수정] 응답 헤더에서 Authorization 토큰 추출
    const token = response.headers['authorization'];
    if (token) {
      localStorage.setItem('accessToken', token);
    }
    
    // 유저 정보 저장
    localStorage.setItem('user', JSON.stringify(response.data));
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
  },

  getToken: (): string | null => {
    return localStorage.getItem('accessToken');
  },

  getCurrentUser: (): User | null => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },
};
