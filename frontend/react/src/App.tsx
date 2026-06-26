import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import AuthPage from './components/authentication/AuthPage'

import { UIProvider } from './components/UIContext'
import LayoutPage from './components/layout/LayoutPage'

export default function App() {

  return (
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
  )
}