import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="brand">VehicleTrace</Link>

          <nav className="nav">
            <NavLink to="/" end>Search</NavLink>
            <NavLink to="/vehicles/new">Register Vehicle</NavLink>
            <NavLink to="/repairs/new">Record Repair</NavLink>
            {user?.role === 'ADMIN' && <NavLink to="/staff">Staff</NavLink>}
          </nav>

          <div className="user-info">
            {user?.fullName} · {user?.garageName}{' '}
            <button className="btn-link" onClick={handleLogout}>Log out</button>
          </div>
        </div>
      </header>

      <main className="container">
        <Outlet />
      </main>
    </>
  )
}