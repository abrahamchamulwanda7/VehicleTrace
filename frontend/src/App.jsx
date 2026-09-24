import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import RegisterGarage from './pages/RegisterGarage'
import Search from './pages/Search'
import VehicleHistory from './pages/VehicleHistory'

// Only lets logged-in staff through; everyone else goes to /login
function RequireLogin({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="container">Loading...</div>
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      {/* Public pages */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<RegisterGarage />} />

      {/* Pages that need login (shown inside the top bar layout) */}
      <Route element={<RequireLogin><Layout /></RequireLogin>}>
        <Route path="/" element={<Search />} />
        <Route path="/vehicles/:plate" element={<VehicleHistory />} />
      </Route>

      {/* Unknown address: go to the start page */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}