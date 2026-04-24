import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, CheckCircle, XCircle, ChevronRight, Star } from 'lucide-react';
import { authService } from '../services/authService';
import { analysisService } from '../services/analysisService';
import type { HistoryResponse, PageResponse } from '../services/analysisService';
import type { User } from '../types/auth';

const History: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [history, setHistory] = useState<PageResponse<HistoryResponse> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const currentUser = authService.getCurrentUser();
    if (!currentUser) {
      navigate('/login');
    } else {
      setUser(currentUser);
      fetchHistory();
    }
  }, [navigate]);

  const fetchHistory = async (page = 0) => {
    setIsLoading(true);
    try {
      const data = await analysisService.getHistory(page);
      setHistory(data);
    } catch (err) {
      console.error('Failed to fetch history', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-gray-100 p-8 font-sans">
      <div className="max-w-5xl mx-auto bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100">
        <div className="bg-gradient-to-r from-indigo-700 to-blue-600 p-8 flex justify-between items-center text-white">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate('/')} className="p-2 hover:bg-white/10 rounded-xl transition">
              <ArrowLeft className="w-6 h-6" />
            </button>
            <h1 className="text-3xl font-black italic tracking-tighter">Analysis History</h1>
          </div>
          <div className="flex items-center gap-4">
            <button onClick={() => navigate('/')} className="bg-white/10 hover:bg-white/20 px-4 py-2 rounded-xl transition font-bold text-sm">New Analysis</button>
            <button onClick={() => { authService.logout(); navigate('/login'); }} className="bg-white/10 hover:bg-white/20 px-4 py-2 rounded-xl transition font-bold text-sm">Logout</button>
          </div>
        </div>

        <div className="p-8">
          {isLoading && !history ? (
            <div className="text-center py-20">
              <div className="animate-spin w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full mx-auto mb-4"></div>
              <p className="font-bold text-gray-400 uppercase tracking-widest">Loading records...</p>
            </div>
          ) : history?.content.length === 0 ? (
            <div className="text-center py-20 bg-gray-50 rounded-3xl border border-dashed border-gray-200">
              <Clock className="w-16 h-16 text-gray-200 mx-auto mb-4" />
              <p className="text-xl font-black text-gray-400 mb-6">NO HISTORY FOUND</p>
              <button onClick={() => navigate('/')} className="bg-indigo-600 text-white px-8 py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-700 transition-all">START YOUR FIRST ANALYSIS</button>
            </div>
          ) : (
            <div className="space-y-4">
              {history?.content.map((item) => (
                <div key={item.id} className="group bg-white border border-gray-100 p-4 rounded-2xl shadow-sm hover:shadow-md transition-all flex items-center gap-6">
                  <div className="w-24 h-24 rounded-xl overflow-hidden shadow-inner flex-shrink-0 bg-gray-100">
                    <img src={item.imageUrl} alt="Analyzed" className="w-full h-full object-cover" />
                  </div>
                  
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-1">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-tighter ${
                        item.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-700' : 
                        item.status === 'FAILED' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'
                      }`}>
                        {item.status}
                      </span>
                      <span className="text-gray-400 text-xs font-bold">{new Date(item.createdAt).toLocaleDateString()}</span>
                    </div>
                    
                    <h3 className="text-lg font-black text-slate-800 flex items-center gap-2">
                      Analysis #{item.id}
                      {item.isFavorite && <Star className="w-4 h-4 text-amber-400 fill-amber-400" />}
                    </h3>
                    
                    {item.status === 'COMPLETED' && (
                      <div className="flex items-center gap-4 mt-2">
                        <div className="flex items-center gap-1">
                          {item.matched ? <CheckCircle className="w-4 h-4 text-emerald-500" /> : <XCircle className="w-4 h-4 text-rose-500" />}
                          <span className={`text-xs font-bold ${item.matched ? 'text-emerald-600' : 'text-rose-600'}`}>
                            {item.matched ? 'MATCHED' : 'NOT MATCHED'}
                          </span>
                        </div>
                        <div className="text-xs font-bold text-slate-400">
                          SCORE: <span className="text-slate-900">{(item.similarityScore || 0).toFixed(1)}%</span>
                        </div>
                      </div>
                    )}
                  </div>

                  <button className="p-3 bg-gray-50 text-gray-400 rounded-xl group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-all">
                    <ChevronRight className="w-6 h-6" />
                  </button>
                </div>
              ))}
              
              {/* Simple Pagination */}
              {history && history.totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-8">
                  {Array.from({ length: history.totalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => fetchHistory(i)}
                      className={`w-10 h-10 rounded-xl font-bold transition-all ${
                        history.number === i ? 'bg-indigo-600 text-white shadow-lg' : 'bg-white text-gray-500 border border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default History;
