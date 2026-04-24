export type AnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface TargetColor {
  hex: string;
}

export interface DetectedColor {
  hex: string;
  ratio: number;
}

export interface AnalysisRequest {
  id: number;
  imageUrl: string;
  status: AnalysisStatus;
  createdAt: string;
  isFavorite: boolean;
}

export interface AnalysisResult {
  id: number;
  analysisId: number;
  similarityScore: number;
  matched: boolean;
  detectedColors: DetectedColor[];
}

export interface MissionResponse {
  requestId: number;
  status: AnalysisStatus;
}

export interface DetectedColorResponse {
  hex: string;
  ratio: number;
}

export interface AnalysisStatusResponse {
  requestId: number;
  status: AnalysisStatus;
  targetSentiment: string;
  matched?: boolean;
  similarityScore?: number;
  colorPalettes?: DetectedColorResponse[]; // 필드명 변경 적용
  message?: string;
}
