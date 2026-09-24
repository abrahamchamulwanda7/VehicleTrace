import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'

export default function RegisterVehicle() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [form, setForm] = useState({
    numberPlate: searchParams.get('plate') || '',
    make: '',
    model: '',
    ownerName: '',
    ownerPhone: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function update(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const data = await api('/vehicles', { method: 'POST', body: form })
      navigate(`/vehicles/${encodeURIComponent(data.numberPlate)}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Register vehicle</h1>
      <p className="muted">
        Register a vehicle once. Every garage on VehicleTrace will then see its repair history.
      </p>

      <div className="card">
        {error && <div className="alert alert-error">{error}</div>}

        <form className="form" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="numberPlate">Number plate</label>
            <input
              id="numberPlate"
              name="numberPlate"
              value={form.numberPlate}
              onChange={update}
              placeholder="e.g. ABC 1234"
              style={{ textTransform: 'uppercase' }}
              required
            />
          </div>

          <div className="grid-2">
            <div className="field">
              <label htmlFor="make">Make</label>
              <input id="make" name="make" value={form.make} onChange={update}
                     placeholder="e.g. Toyota" required />
            </div>
            <div className="field">
              <label htmlFor="model">Model</label>
              <input id="model" name="model" value={form.model} onChange={update}
                     placeholder="e.g. Corolla" required />
            </div>
          </div>

          <div className="grid-2">
            <div className="field">
              <label htmlFor="ownerName">Owner's name</label>
              <input id="ownerName" name="ownerName" value={form.ownerName} onChange={update} required />
            </div>
            <div className="field">
              <label htmlFor="ownerPhone">Owner's phone number</label>
              <input id="ownerPhone" name="ownerPhone" type="tel" value={form.ownerPhone}
                     onChange={update} required />
            </div>
          </div>

          <button className="btn" type="submit" disabled={busy}>
            {busy ? 'Saving...' : 'Register vehicle'}
          </button>
        </form>
      </div>
    </>
  )
}