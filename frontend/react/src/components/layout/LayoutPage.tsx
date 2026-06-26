import { Outlet, useNavigate } from 'react-router-dom'
import { Bell, User } from 'lucide-react'
import { logout } from '../../api/auth'
import { useUIContext } from '../UIContext';
import Dropdown from '../util/Dropdown';
import Header from './Header';

export default function LayoutPage() {
  const { closePanel, togglePanel } = useUIContext();

  const navigate = useNavigate();

  const handleLogout = async (): Promise<void> => {
    const response = await logout();

    if(response){
      navigate("/");
    }
  }

  const handleGeneralClick = (event: React.MouseEvent<HTMLDivElement>): void => {
    event.stopPropagation();
    closePanel();
  }
  

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

      <button onClick={handleLogout}></button>

    </div>
  )
}