import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api'

// Today's date as YYYY-MM-DD (local time)
function today() {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

export default function RecordRepair() {
  const [searchParams] = useSearchParams()

  const [form, setForm] = useState({
    numberPlate: searchParams.get('plate') || '',
    problemDescription: '',
    workDone: '',
    cost: '',
    repairDate: today(),
  })
  const [parts, setParts] = useState([])
  const [error, setError] = useState(null)   // { message, status }
  const [result, setResult] = useState(null) // response after saving
  const [busy, setBusy] = useState(false)

  function update(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  // ----- Parts list (FR7) -----
  function addPart() {
    setParts([...parts, { partName: '', quantity: 1, cost: '' }])
  }
  function updatePart(index, field, value) {
    setParts(parts.map((p, i) => (i === index ? { ...p, [field]: value } : p)))
  }
  function removePart(index) {
    setParts(parts.filter((_, i) => i !== index))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const body = {
        ...form,
        cost: Number(form.cost),
        parts: parts
          .filter((p) => p.partName.trim() !== '')
          .map((p) => ({
            partName: p.partName,
            quantity: Number(p.quantity),
            cost: Number(p.cost),
          })),
      }
      const data = await api('/repairs', { method: 'POST', body })
      setResult(data)
      window.scrollTo(0, 0)
    } catch (err) {
      setError({ message: err.message, status: err.status })
    } finally {
      setBusy(false)
    }
  }

  function recordAnother() {
    setResult(null)
    setForm({ ...form, problemDescription: '', workDone: '', cost: '', repairDate: today() })
    setParts([])
  }

  // ----- After saving: show the result and any repeat-problem warning (FR8) -----
  if (result) {
    return (
      <>
        <h1>Repair recorded</h1>

        {result.isRepeatProblem ? (
          <div className="alert alert-repeat">
            <div style={{ fontSize: '1.1rem' }}>⚠ {result.warning}</div>
            <ul style={{ margin: '8px 0 0', paddingLeft: 20, fontWeight: 400 }}>
              {result.matchingRepairs.map((m) => (
                <li key={m.repairId}>
                  <strong>{m.repairDate}</strong> at {m.garageName} ({m.province}): {m.problemDescription}
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <div className="alert alert-success">
            Repair saved. No previous similar problem was found for this vehicle.
          </div>
        )}

        <div className="card">
          <p><strong>Vehicle:</strong> {result.numberPlate}</p>
          <p><strong>Garage:</strong> {result.garageName}</p>
          <p><strong>Date:</strong> {result.repairDate}</p>
          <p><strong>Parts recorded:</strong> {result.partsRecorded}</p>
          <p><strong>Recorded by:</strong> {result.recordedBy}</p>
          <div className="actions">
            <Link className="btn" to={`/vehicles/${encodeURIComponent(result.numberPlate)}`}>
              View vehicle history
            </Link>
            <button className="btn btn-secondary" onClick={recordAnother}>
              Record another repair
            </button>
          </div>
        </div>
      </>
    )
  }

  // ----- The repair form (FR6) -----
  return (
    <>
      <h1>Record repair</h1>
      <p className="muted">
        The repair is saved under your garage. VehicleTrace checks it against the vehicle's
        history at all garages and warns you about repeat problems.
      </p>

      <div className="card">
        {error && (
          <div className="alert alert-error">
            {error.message}
            {error.status === 404 && (
              <>
                {' '}
                <Link to={`/vehicles/new?plate=${encodeURIComponent(form.numberPlate)}`}>
                  Register this vehicle
                </Link>
              </>
            )}
          </div>
        )}

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

          <div className="field">
            <label htmlFor="problemDescription">Problem description</label>
            <textarea
              id="problemDescription"
              name="problemDescription"
              value={form.problemDescription}
              onChange={update}
              placeholder="e.g. Front brakes squeaking loudly"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="workDone">Work done</label>
            <textarea
              id="workDone"
              name="workDone"
              value={form.workDone}
              onChange={update}
              placeholder="e.g. Replaced front brake pads"
              required
            />
          </div>

          <div className="grid-2">
            <div className="field">
              <label htmlFor="cost">Repair cost (K)</label>
              <input
                id="cost"
                name="cost"
                type="number"
                min="0"
                step="0.01"
                value={form.cost}
                onChange={update}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="repairDate">Repair date</label>
              <input
                id="repairDate"
                name="repairDate"
                type="date"
                max={today()}
                value={form.repairDate}
                onChange={update}
                required
              />
            </div>
          </div>

          {/* Parts used (FR7) */}
          <h3 style={{ marginTop: 8 }}>Parts used</h3>
          {parts.length === 0 && <p className="muted">No parts added.</p>}

          {parts.map((part, index) => (
            <div key={index} style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'flex-end' }}>
              <div className="field" style={{ flex: '3 1 200px' }}>
                <label>Part name</label>
                <input
                  value={part.partName}
                  onChange={(e) => updatePart(index, 'partName', e.target.value)}
                  required
                />
              </div>
              <div className="field" style={{ flex: '1 1 80px' }}>
                <label>Qty</label>
                <input
                  type="number"
                  min="1"
                  value={part.quantity}
                  onChange={(e) => updatePart(index, 'quantity', e.target.value)}
                  required
                />
              </div>
              <div className="field" style={{ flex: '1 1 110px' }}>
                <label>Cost (K)</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={part.cost}
                  onChange={(e) => updatePart(index, 'cost', e.target.value)}
                  required
                />
              </div>
              <button
                type="button"
                className="btn btn-secondary btn-small"
                onClick={() => removePart(index)}
              >
                Remove
              </button>
            </div>
          ))}

          <div>
            <button type="button" className="btn btn-secondary" onClick={addPart}>
              + Add part
            </button>
          </div>

          <button className="btn" type="submit" disabled={busy}>
            {busy ? 'Saving...' : 'Save repair'}
          </button>
        </form>
      </div>
    </>
  )
}