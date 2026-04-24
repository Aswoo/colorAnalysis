import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Palette, Clock } from 'lucide-react';
import { authService } from '../services/authService';
import { analysisService } from '../services/analysisService';
import type { User } from '../types/auth';
import type { AnalysisStatusResponse } from '../types/analysis';

const Home: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [targetSentiment, setTargetSentiment] = useState('Green');
  const [isUploading, setIsUploading] = useState(false);
  const [analysisResult, setAnalysisResult] = useState<AnalysisStatusResponse | null>(null);
  const [, setOptimizationStatus] = useState<string | null>(null);
  
  const navigate = useNavigate();

  useEffect(() => {
    const currentUser = authService.getCurrentUser();
    if (!currentUser) {
      navigate('/login');
    } else {
      setUser(currentUser);
    }
  }, [navigate]);

  const optimizeImage = (file: File): Promise<File> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = (event) => {
        const img = new Image();
        img.src = event.target?.result as string;
        img.onload = () => {
          const canvas = document.createElement('canvas');
          const MAX_WIDTH = 1200;
          const MAX_HEIGHT = 1200;
          let width = img.width;
          let height = img.height;
          if (width > height) {
            if (width > MAX_WIDTH) { height *= MAX_WIDTH / width; width = MAX_WIDTH; }
          } else {
            if (height > MAX_HEIGHT) { width *= MAX_HEIGHT / height; height = MAX_HEIGHT; }
          }
          canvas.width = width; canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx?.drawImage(img, 0, 0, width, height);
          canvas.toBlob((blob) => {
            if (blob) {
              resolve(new File([blob], file.name.replace(/\.[^/.]+$/, "") + ".jpg", { type: 'image/jpeg' }));
            } else { reject(new Error('fail')); }
          }, 'image/jpeg', 0.8);
        };
      };
    });
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setOptimizationStatus('처리 중...');
      try {
        const optimizedFile = await optimizeImage(e.target.files[0]);
        setFile(optimizedFile);
        setPreview(URL.createObjectURL(optimizedFile));
        setAnalysisResult(null);
        setOptimizationStatus(null);
      } catch (err) { alert('오류'); setOptimizationStatus(null); }
    }
  };

  const handleUpload = async () => {
    if (!file || !user) return;
    setIsUploading(true);
    setAnalysisResult(null);
    try {
      const { requestId } = await analysisService.performAnalysis(user.id, targetSentiment, file);
      const pollInterval = setInterval(async () => {
        try {
          const status = await analysisService.getAnalysisStatus(requestId, targetSentiment);
          console.log("Analysis Result Object:", status); // 상세 로깅
          setAnalysisResult(status);
          if (status.status === 'COMPLETED' || status.status === 'FAILED') {
            clearInterval(pollInterval);
            setIsUploading(false);
          }
        } catch (e) { clearInterval(pollInterval); setIsUploading(false); }
      }, 2500);
    } catch (err) { setIsUploading(false); }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-gray-100 p-8 font-sans">
      <div className="max-w-5xl mx-auto bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100">
        <div className="bg-gradient-to-r from-indigo-700 to-blue-600 p-8 flex justify-between items-center text-white">
          <h1 className="text-3xl font-black italic tracking-tighter">findColor</h1>
          <div className="flex gap-4">
            <button onClick={() => navigate('/history')} className="bg-white/10 hover:bg-white/20 px-4 py-2 rounded-xl transition flex items-center gap-2 font-bold text-sm">
              <Clock className="w-4 h-4" /> History
            </button>
            <button onClick={() => { authService.logout(); navigate('/login'); }} className="bg-white/10 hover:bg-white/20 px-4 py-2 rounded-xl transition font-bold text-sm">Logout</button>
          </div>
        </div>

        <div className="p-8 grid grid-cols-1 lg:grid-cols-12 gap-10">
          <div className="lg:col-span-5 space-y-8">
            <div className="border-3 border-dashed border-gray-200 rounded-3xl p-4 text-center relative bg-gray-50/50">
              <input type="file" accept="image/*" onChange={handleFileChange} className="absolute inset-0 opacity-0 cursor-pointer z-10" />
              {preview ? <img src={preview} alt="미리보기" className="max-h-96 w-full object-cover rounded-2xl shadow-xl" /> : <div className="py-24 font-bold text-gray-400 text-lg">CLICK OR DRAG IMAGE</div>}
            </div>
            <div className="bg-gray-50 p-6 rounded-3xl border border-gray-100 grid grid-cols-3 gap-3">
              {['Red', 'Green', 'Blue', 'Yellow', 'Purple', 'Pink'].map((color) => (
                <button key={color} onClick={() => setTargetSentiment(color)} className={`py-3 rounded-xl text-sm font-bold transition-all ${targetSentiment === color ? 'bg-indigo-600 text-white shadow-lg' : 'bg-white text-gray-500 border border-gray-200'}`}>{color}</button>
              ))}
            </div>
            <button onClick={handleUpload} disabled={!file || isUploading} className={`w-full py-5 rounded-2xl font-black text-xl shadow-2xl transition-all ${!file || isUploading ? 'bg-gray-200 text-gray-400' : 'bg-indigo-600 text-white hover:bg-indigo-700'}`}>{isUploading ? 'ANALYZING...' : 'START ANALYSIS'}</button>
          </div>

          <div className="lg:col-span-7 bg-slate-900 rounded-3xl p-8 text-white shadow-2xl overflow-y-auto max-h-[800px]">
            <h2 className="text-2xl font-black flex items-center gap-3 mb-8"><Palette className="text-indigo-400" /> Analysis Report</h2>
            {isUploading && !analysisResult && <div className="text-center py-32 animate-pulse text-indigo-400 font-black text-2xl uppercase">Engine Loading...</div>}
            {analysisResult && analysisResult.status === 'FAILED' && (
              <div className="p-6 rounded-3xl bg-red-500/10 border border-red-500/30 text-center">
                <p className="text-red-400 font-black text-xl">ANALYSIS FAILED</p>
                <p className="text-slate-400 text-sm mt-2">{analysisResult.message || '분석 중 오류가 발생했습니다.'}</p>
              </div>
            )}
            {analysisResult && analysisResult.status === 'COMPLETED' && (
              <div className="space-y-8 animate-in fade-in zoom-in-95 duration-500">
                <div className={`p-6 rounded-3xl flex items-center gap-6 ${analysisResult.matched ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-rose-500/10 border border-rose-500/30'}`}>
                  {analysisResult.matched ? <CheckCircle className="w-10 h-10 text-emerald-400" /> : <XCircle className="w-10 h-10 text-rose-400" />}
                  <div><h3 className="text-3xl font-black">{analysisResult.matched ? 'SUCCESS' : 'FAILED'}</h3><p className="text-slate-400 text-xs font-bold uppercase">{targetSentiment} SENTIMENT</p></div>
                </div>
                <div className="bg-slate-800/50 p-6 rounded-3xl border border-slate-700/50">
                   <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-2">Accuracy Score</p>
                   <p className="text-4xl font-black">{(analysisResult.similarityScore || 0).toFixed(1)}%</p>
                </div>
                <div className="space-y-4">
                  <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Color Palette (Top 8)</p>
                  {analysisResult.colorPalettes && analysisResult.colorPalettes.length > 0 ? (
                    analysisResult.colorPalettes.map((c, i) => (
                      <div key={i} className="flex items-center gap-4 bg-slate-800/30 p-3 rounded-2xl border border-slate-700/30 group transition-all hover:bg-slate-800/60">
                        <div className="w-14 h-14 rounded-xl shadow-lg border-2 border-white/5" style={{ backgroundColor: c.hex }}></div>
                        <div className="flex-1">
                          <div className="flex justify-between mb-1.5 font-bold"><span className="text-xs font-mono uppercase text-slate-400">{c.hex}</span><span className="text-sm">{(c.ratio * 100).toFixed(1)}%</span></div>
                          <div className="w-full bg-slate-900 h-2 rounded-full overflow-hidden"><div className="bg-indigo-500 h-full rounded-full" style={{ width: `${c.ratio * 100}%` }}></div></div>
                        </div>
                      </div>
                    ))
                  ) : <div className="text-center py-10 text-slate-600 font-bold border border-dashed border-slate-800 rounded-3xl">NO PALETTE DATA RETURNED</div>}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
