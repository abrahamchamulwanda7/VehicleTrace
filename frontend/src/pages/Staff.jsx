import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export default function Staff() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [list, setList] = useState(null)
  const [listError, setListError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  const [form, setForm] = useState({ fullName: '', username: '', password: '', role: 'STAFF' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  // Load this garage's active staff (admin only)
  useEffect(() => {
    if (!isAdmin) return
    let active = true
    api('/users')
      .then((data) => { if (active) setList(data) })
      .catch((err) => { if (active) setListError(err.message) })
    return () => { active = false }
  }, [isAdmin, reloadKey])

  if (!isAdmin) {
    return <div className="alert alert-error">Only the garage admin can manage staff accounts.</div>
  }

  function update(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  // Add a staff member
  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setBusy(true)
    try {
      const data = await api('/users', { method: 'POST', body: form })
      setSuccess(`Account created for ${data.fullName} (username: ${data.username}).`)
      setForm({ fullName: '', username: '', password: '', role: 'STAFF' })
      setReloadKey((k) => k + 1) // refresh the list
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  // Remove (deactivate) a staff member
  async function handleRemove(staff) {
    const ok = window.confirm(
      `Remove ${staff.fullName} (${staff.username})?\n\n` +
      'They will no longer be able to log in. Repairs they recorded will stay in the history.'
    )
    if (!ok) return

    setError('')
    setSuccess('')
    try {
      await api(`/users/${staff.userId}`, { method: 'DELETE' })
      setSuccess(`${staff.fullName} has been removed.`)
      setReloadKey((k) => k + 1) // refresh the list
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1>Staff</h1>
      <p className="muted">Manage logins for staff at {user.garageName}.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add a staff member */}
      <div className="card">
        <h2>Add staff member</h2>

        <form className="form" onSubmit={handleSubmit}>
          <div className="grid-2">
            <div className="field">
              <label htmlFor="fullName">Full name</label>
              <input id="fullName" name="fullName" value={form.fullName} onChange={update} required />
            </div>
            <div className="field">
              <label htmlFor="username">Username</label>
              <input id="username" name="username" value={form.username} onChange={update}
                     autoComplete="off" minLength={3} required />
            </div>
          </div>

          <div className="grid-2">
            <div className="field">
              <label htmlFor="password">Password</label>
              <input id="password" name="password" type="password" value={form.password}
                     onChange={update} autoComplete="new-password" minLength={6} required />
            </div>
            <div className="field">
              <label htmlFor="role">Role</label>
              <select id="role" name="role" value={form.role} onChange={update}>
                <option value="STAFF">Staff</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          </div>

          <button className="btn" type="submit" disabled={busy}>
            {busy ? 'Adding...' : 'Add staff member'}
          </button>
        </form>
      </div>

      {/* Staff list */}
      <div className="card">
        <h2>Current staff</h2>
        {listError && <div className="alert alert-error">{listError}</div>}
        {!list && !listError && <p>Loading...</p>}
        {list && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Name</th><th>Username</th><th>Role</th><th>Added</th><th></th></tr>
              </thead>
              <tbody>
                {list.users.map((u) => (
                  <tr key={u.userId}>
                    <td>{u.fullName}</td>
                    <td>{u.username}</td>
                    <td><span className="badge">{u.role}</span></td>
                    <td>{u.createdAt?.slice(0, 10)}</td>
                    <td>
                      {u.userId === user.userId ? (
                        <span className="muted">(you)</span>
                      ) : (
                        <button
                          className="btn btn-secondary btn-small"
                          onClick={() => handleRemove(u)}
                        >
                          Remove
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}