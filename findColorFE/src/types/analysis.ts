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
