import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import Signup from './pages/Signup'
import './index.css'

console.log("main.tsx: Rendering Signup with Router...");

const rootElement = document.getElementById('root');
if (rootElement) {
  try {
    createRoot(rootElement).render(
      <BrowserRouter>
        <div className="min-h-screen bg-gray-100">
          <Signup />
        </div>
      </BrowserRouter>
    );
    console.log("main.tsx: Render successful");
  } catch (error) {
    console.error("main.tsx: Render error:", error);
    rootElement.innerHTML = `<div style="color: red; padding: 20px;">
      <h2>렌더링 에러 발생</h2>
      <pre>${error}</pre>
    </div>`;
  }
}
