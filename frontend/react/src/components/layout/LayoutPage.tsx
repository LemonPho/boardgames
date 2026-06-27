import { Outlet } from 'react-router-dom'
import { useUIContext } from '../../context/UIContext';
import Header from './Header';
import { useAuthenticationContext } from '../../context/AuthenticationContext';
import { useEffect } from 'react';
import { useAlertsContext } from '../../context/AlertsContext';

export default function LayoutPage() {
  const { errorMessage, successMessage, infoMessage } = useAlertsContext();
  const { closePanel } = useUIContext();
  const { csrfInit } = useAuthenticationContext();

  const handleGeneralClick = (event: React.MouseEvent<HTMLDivElement>): void => {
    event.stopPropagation();
    closePanel();
  }

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      await csrfInit();
    }

    fetchData();
  }, []);
  

  return (
    <div className="min-h-screen flex flex-col bg-gray-50" onClick={handleGeneralClick}>

      <Header/>

      {/* Alerts */}
      <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 w-80">
        {errorMessage && (
          <div className="flex items-start gap-3 bg-white border border-red-200 text-red-700 rounded-xl shadow-lg px-4 py-3">
            <span className="text-red-400 mt-0.5">✕</span>
            <p className="text-sm">{errorMessage}</p>
          </div>
        )}
        {successMessage && (
          <div className="flex items-start gap-3 bg-white border border-green-200 text-green-700 rounded-xl shadow-lg px-4 py-3">
            <span className="text-green-400 mt-0.5">✓</span>
            <p className="text-sm">{successMessage}</p>
          </div>
        )}
        {infoMessage && (
          <div className="flex items-start gap-3 bg-white border border-blue-200 text-blue-700 rounded-xl shadow-lg px-4 py-3">
            <span className="text-blue-400 mt-0.5">ℹ</span>
            <p className="text-sm">{infoMessage}</p>
          </div>
        )}
      </div>

      {/* Main content */}
      <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t border-gray-200 bg-white px-6 py-4 text-center text-sm text-gray-400">
        Boardgames © {new Date().getFullYear()}
      </footer>

    </div>
  )
}