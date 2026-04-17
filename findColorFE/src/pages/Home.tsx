import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import { User } from '../types/auth';

const Home: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const currentUser = authService.getCurrentUser();
    if (!currentUser) {
      navigate('/login');
    } else {
      setUser(currentUser);
    }
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-2xl font-bold text-gray-800">findColor 대시보드</h1>
          <div className="flex items-center gap-4">
            <span className="text-gray-600 font-medium">{user.nickname}님 환영합니다!</span>
            <button
              onClick={handleLogout}
              className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition"
            >
              로그아웃
            </button>
          </div>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
            <p className="text-gray-500 mb-4">이미지를 업로드하여 색상을 분석해보세요.</p>
            <button className="px-6 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700">
              이미지 업로드 (준비 중)
            </button>
          </div>
          
          <div className="bg-gray-50 rounded-lg p-6">
            <h2 className="text-lg font-semibold mb-4 text-gray-700">최근 분석 히스토리</h2>
            <p className="text-gray-500 italic">아직 분석 데이터가 없습니다.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
