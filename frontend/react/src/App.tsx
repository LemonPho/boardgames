import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import AuthPage from './components/authentication/AuthPage'
import HomePage from './components/home/HomePage'
import LayoutPage from './components/layout/LayoutPage'

import { UIProvider } from './context/UIContext'
import { AlertsContextProvider } from './context/AlertsContext'
import { UserContextProvider } from './context/UserContext'
import { AuthenticationContextProvider } from './context/AuthenticationContext'
import { NotificationsContextProvider } from './context/NotificationsContext'
import GamePage from './components/games/GamePage'
import WaitingRoom from './components/rooms/waiting-room/WaitingRoom'
import GameSession from './components/rooms/game-session/GameSession'
import FinalScoreboard from './components/rooms/FinalScoreboard'
import RoomPage from './components/rooms/RoomPage'
import AcceptInvitePage from './components/rooms/waiting-room/AcceptInvitePage'
import VerifyPage from './components/authentication/VerifyPage'
import ProfilePage from './components/user/ProfilePage'
import SettingsPage from './components/user/SettingsPage'

export default function App() {

  return (
    <AlertsContextProvider>
      <UserContextProvider>
        <UIProvider>
          <BrowserRouter>
            <AuthenticationContextProvider>
              <NotificationsContextProvider>
                <Routes>
                  <Route path="/" element={<LayoutPage />}>
                    <Route path="" element={<HomePage />} />
                    <Route path="register" element={<AuthPage tab="register" />} />
                    <Route path="login" element={<AuthPage tab="login" />} />
                    <Route path="games/:name" element={<GamePage />} />
                    <Route path="profile/:username" element={<ProfilePage />} />
                    <Route path="settings" element={<SettingsPage />} />
                    <Route path="scoreboard/:name" element={<FinalScoreboard />} />
                    <Route path="/rooms/accept" element={<AcceptInvitePage />} />
                    <Route path="verify" element={<VerifyPage />} />
                    <Route path="rooms/:name" element={<RoomPage />}>
                      <Route path="waiting" element={<WaitingRoom />} />
                      <Route path="in-progress" element={<GameSession />} />
                      <Route path="final" element={<FinalScoreboard />} />
                    </Route>
                  </Route>
                </Routes>
              </NotificationsContextProvider>
            </AuthenticationContextProvider>
          </BrowserRouter>
        </UIProvider>
      </UserContextProvider>
    </AlertsContextProvider >
  )
}