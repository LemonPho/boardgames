import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import AuthPage from './components/authentication/AuthPage'
import HomePage from './components/home/HomePage'
import LayoutPage from './components/layout/LayoutPage'

import { UIProvider } from './context/UIContext'
import { AlertsContextProvider } from './context/AlertsContext'
import { ApplicationContextProvider } from './context/ApplicationContext'
import { AuthenticationContextProvider } from './context/AuthenticationContext'
import GamePage from './components/games/GamePage'

export default function App() {

  return (
    <AlertsContextProvider>
      <AuthenticationContextProvider>
        <ApplicationContextProvider>
          <UIProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<LayoutPage/>}>
                  <Route path="" element={<HomePage />}/>
                  <Route path="register" element={<AuthPage tab="register"/>} />
                  <Route path="login" element={<AuthPage tab="login"/>}/>
                  <Route path="games/:name" element={<GamePage/>}/>
                </Route>
              </Routes>
            </BrowserRouter>
          </UIProvider>
        </ApplicationContextProvider>
      </AuthenticationContextProvider>
    </AlertsContextProvider>
  )
}