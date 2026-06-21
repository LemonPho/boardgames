import './App.css'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import RegisterPage from './components/authentication/RegisterPage'

export default function App() {

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        {/*<Route path="/login" element={<LoginPage />} />*/}
        {/*<Route path="/verify" element={<VerifyPage />} />*/}
      </Routes>
    </BrowserRouter>
  )
}