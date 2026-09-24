import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function Search() {
  const navigate = useNavigate()
  const [plate, setPlate] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    const cleaned = plate.trim()
    if (!cleaned) return
    navigate(`/vehicles/${encodeURIComponent(cleaned)}`)
  }

  return (
    <>
      <h1>Search vehicle</h1>
      <p className="muted">
        Enter a number plate to see the vehicle's full repair history from all garages.
      </p>

      <div className="card">
        <form className="form" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="plate">Number plate</label>
            <input
              id="plate"
              value={plate}
              onChange={(e) => setPlate(e.target.value)}
              placeholder="e.g. ABC 1234"
              style={{ fontSize: '1.2rem', textTransform: 'uppercase' }}
              autoFocus
              required
            />
          </div>
          <button className="btn" type="submit">Search</button>
        </form>
      </div>
    </>
  )
}