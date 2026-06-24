import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import AuthPage from './components/authentication/AuthPage'

export default function App() {

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/register" element={<AuthPage tab="register"/>} />
        <Route path="/login" element={<AuthPage tab="login"/>}/>
      </Routes>
    </BrowserRouter>
  )
}