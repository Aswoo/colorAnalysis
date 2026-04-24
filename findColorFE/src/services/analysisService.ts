import apiClient from './apiClient'; // 공통 클라이언트 사용
import type { AnalysisStatusResponse } from '../types/analysis';

export interface HistoryResponse {
  id: number;
  imageUrl: string;
  status: string;
  similarityScore: number | null;
  matched: boolean | null;
  createdAt: string;
  isFavorite: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export const analysisService = {
  // 1. 이미지 분석 요청 (이제 헤더 주입이 자동화됨)
  performAnalysis: async (userId: number, targetSentiment: string, file: File) => {
    const formData = new FormData();
    const requestData = JSON.stringify({ userId, targetSentiment });
    formData.append('request', new Blob([requestData], { type: 'application/json' }));
    formData.append('file', file);

    const response = await apiClient.post('/images/perform', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // 2. 분석 상태 및 결과 조회
  getAnalysisStatus: async (requestId: number, targetSentiment: string): Promise<AnalysisStatusResponse> => {
    // 캐시 방지를 위해 타임스탬프 추가
    const response = await apiClient.get<AnalysisStatusResponse>(`/images/analysis/${requestId}`, {
      params: { targetSentiment, _t: Date.now() }
    });
    return response.data;
  },

  // 3. 히스토리 조회
  getHistory: async (page = 0, size = 20): Promise<PageResponse<HistoryResponse>> => {
    const response = await apiClient.get<PageResponse<HistoryResponse>>('/analysis/history', {
      params: { page, size }
    });
    return response.data;
  }
};
