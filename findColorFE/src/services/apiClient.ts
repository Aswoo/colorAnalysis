import axios from 'axios';
import { authService } from './authService';

const API_BASE_URL = 'http://localhost:8080/api';

// 모든 API 요청에서 공통으로 사용할 Axios 인스턴스
const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

// [Request Interceptor] 모든 요청 전에 실행
apiClient.interceptors.request.use(
  (config) => {
    const token = authService.getToken();
    if (token) {
      // 헤더에 자동으로 토큰 주입 (Bearer 포함)
      config.headers.Authorization = token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// [Response Interceptor] 서버 응답 직후에 실행
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 토큰 만료 등 인증 에러(401) 발생 시
    if (error.response?.status === 401) {
      authService.logout();
      window.location.href = '/login'; // 강제 로그아웃 및 이동
    }
    return Promise.reject(error);
  }
);

export default apiClient;
