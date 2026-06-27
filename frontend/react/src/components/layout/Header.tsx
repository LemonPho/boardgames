import { Bell, User } from "lucide-react";
import { useUIContext } from "../../context/UIContext";
import Dropdown from "../util/Dropdown";
import { useAuthenticationContext } from "../../context/AuthenticationContext";

export default function Header() {
  const { togglePanel, closePanel } = useUIContext();
  const { logoutUser, deleteAccessToken, restoreSession } = useAuthenticationContext();

  const handleLogout = async (): Promise<void> => {
    await logoutUser();
    closePanel();
  }

  return(
    <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <span className="font-semibold text-gray-800 text-lg">Boardgames</span>

      <div className="flex items-center gap-2">
      {/* Notification bell */}
        <div className="relative">
          <button 
            onClick={(event) => {togglePanel('notifications'); event.stopPropagation();}}
            className="w-9 h-9 flex items-center justify-center border border-gray-200 rounded-lg hover:bg-gray-50 transition relative"
          >
            <Bell size={18} className="text-gray-600" />
          </button>
          <Dropdown id="notifications">
            <span className="w-full text-left px-4 py-2 text-sm text-gray-600 hover:bg-gray-50">No notifications...</span>
          </Dropdown>

        </div>
        
        {/* Profile */}
        <div className="relative">
          <button
            onClick={(event) => {togglePanel('profile'); event.stopPropagation();}}
            className="w-9 h-9 flex items-center justify-center border border-gray-200 rounded-lg hover:bg-gray-50 transition"
          >
            <User size={18} className="text-gray-600" />
          </button>

          <Dropdown id="profile">
            <button className="w-full text-left px-4 py-2 text-sm text-gray-600 hover:bg-gray-50">Profile</button>
            <hr className="border-gray-100" />
            <button 
              onClick={handleLogout}
              className="w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-gray-50"
            >
              Sign out
            </button>

            <hr />
            <button 
              onClick={deleteAccessToken}
              className="w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-gray-50"
            >
              DELETE TOKEN
            </button>

            <button 
              onClick={restoreSession}
              className="w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-gray-50"
            >
              RESTORE SESSION
            </button>
          </Dropdown>
        </div>
      </div>
    </header>
  )
}