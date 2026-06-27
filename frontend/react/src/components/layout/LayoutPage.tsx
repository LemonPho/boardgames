import { Outlet } from 'react-router-dom'
import { useUIContext } from '../../context/UIContext';
import Header from './Header';
import { useAuthenticationContext } from '../../context/AuthenticationContext';
import { useEffect } from 'react';

export default function LayoutPage() {
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