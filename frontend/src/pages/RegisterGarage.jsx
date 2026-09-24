import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export const PROVINCES = [
  'Central', 'Copperbelt', 'Eastern', 'Luapula', 'Lusaka',
  'Muchinga', 'Northern', 'North-Western', 'Southern', 'Western',
]

export default function RegisterGarage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    garageName: '', province: '', address: '', phone: '',
    fullName: '', username: '', password: '', confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function update(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (form.password !== form.confirmPassword) {
      setError('The two passwords do not match')
      return
    }

    setBusy(true)
    try {
      const { confirmPassword, ...data } = form
      await api('/garages/register', { method: 'POST', body: data })
      // Log the new admin in straight away
      await login(form.username, form.password)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-box" style={{ maxWidth: 640 }}>
        <div className="card">
          <h1>Register your garage</h1>
          <p className="muted">Create your garage account and your admin login.</p>

          {error && <div className="alert alert-error">{error}</div>}

          <form className="form" onSubmit={handleSubmit}>
            <h3>Garage details</h3>

            <div className="field">
              <label htmlFor="garageName">Garage name</label>
              <input id="garageName" name="garageName" value={form.garageName} onChange={update} required />
            </div>

            <div className="grid-2">
              <div className="field">
                <label htmlFor="province">Province</label>
                <select id="province" name="province" value={form.province} onChange={update} required>
                  <option value="">Select province</option>
                  {PROVINCES.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="phone">Phone number</label>
                <input id="phone" name="phone" type="tel" value={form.phone} onChange={update} required />
              </div>
            </div>

            <div className="field">
              <label htmlFor="address">Address</label>
              <input id="address" name="address" value={form.address} onChange={update} required />
            </div>

            <h3>Admin login</h3>

            <div className="grid-2">
              <div className="field">
                <label htmlFor="fullName">Your full name</label>
                <input id="fullName" name="fullName" value={form.fullName} onChange={update} required />
              </div>
              <div className="field">
                <label htmlFor="username">Username</label>
                <input id="username" name="username" value={form.username} onChange={update}
                       autoComplete="username" minLength={3} required />
              </div>
            </div>

            <div className="grid-2">
              <div className="field">
                <label htmlFor="password">Password</label>
                <input id="password" name="password" type="password" value={form.password} onChange={update}
                       autoComplete="new-password" minLength={6} required />
              </div>
              <div className="field">
                <label htmlFor="confirmPassword">Confirm password</label>
                <input id="confirmPassword" name="confirmPassword" type="password" value={form.confirmPassword}
                       onChange={update} autoComplete="new-password" minLength={6} required />
              </div>
            </div>

            <button className="btn" type="submit" disabled={busy}>
              {busy ? 'Registering...' : 'Register garage'}
            </button>
          </form>

          <p className="muted" style={{ marginTop: 16 }}>
            Already registered? <Link to="/login">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}