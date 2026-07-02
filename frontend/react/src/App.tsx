import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import AuthPage from './components/authentication/AuthPage'
import HomePage from './components/home/HomePage'
import LayoutPage from './components/layout/LayoutPage'

import { UIProvider } from './context/UIContext'
import { AlertsContextProvider } from './context/AlertsContext'
import { UserContextProvider } from './context/UserContext'
import { AuthenticationContextProvider } from './context/AuthenticationContext'
import GamePage from './components/games/GamePage'
import WaitingRoom from './components/rooms/waiting-room/WaitingRoom'
import GameSession from './components/rooms/GameSession'
import FinalScoreboard from './components/rooms/FinalScoreboard'
import RoomPage from './components/rooms/RoomPage'
import AcceptInvitePage from './components/rooms/waiting-room/AcceptInvitePage'

export default function App() {

  return (
    <AlertsContextProvider>
      <UserContextProvider>
        <UIProvider>
          <BrowserRouter>
            <AuthenticationContextProvider>
              <Routes>
                <Route path="/" element={<LayoutPage />}>
                  <Route path="" element={<HomePage />} />
                  <Route path="register" element={<AuthPage tab="register" />} />
                  <Route path="login" element={<AuthPage tab="login" />} />
                  <Route path="games/:name" element={<GamePage />} />
                  <Route path="/rooms/accept" element={<AcceptInvitePage />} />
                  <Route path="rooms/:name" element={<RoomPage />}>
                    <Route path="waiting" element={<WaitingRoom />} />
                    <Route path="in-progress" element={<GameSession />} />
                    <Route path="final" element={<FinalScoreboard />} />
                  </Route>
                </Route>
              </Routes>
            </AuthenticationContextProvider>
          </BrowserRouter>
        </UIProvider>
      </UserContextProvider>
    </AlertsContextProvider >
  )
}