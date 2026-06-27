import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import AuthPage from './components/authentication/AuthPage'

import { UIProvider } from './context/UIContext'
import LayoutPage from './components/layout/LayoutPage'
import { AlertsContextProvider } from './context/AlertsContext'
import { ApplicationContextProvider } from './context/ApplicationContext'
import { AuthenticationContextProvider } from './context/AuthenticationContext'

export default function App() {

  return (
    <AlertsContextProvider>
      <AuthenticationContextProvider>
        <ApplicationContextProvider>
          <UIProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<LayoutPage/>}>
                  <Route path="register" element={<AuthPage tab="register"/>} />
                  <Route path="login" element={<AuthPage tab="login"/>}/>
                </Route>
              </Routes>
            </BrowserRouter>
          </UIProvider>
        </ApplicationContextProvider>
      </AuthenticationContextProvider>
    </AlertsContextProvider>
  )
}