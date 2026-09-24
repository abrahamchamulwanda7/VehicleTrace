import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'

// Shows money in Zambian Kwacha, e.g. K 350.00
export function money(value) {
  return `K ${Number(value || 0).toFixed(2)}`
}

export default function VehicleHistory() {
  const { plate } = useParams()
  const [result, setResult] = useState(null) // { plate, data, error }

  // Load the vehicle and its history whenever the plate changes
  useEffect(() => {
    let active = true
    api(`/vehicles/${encodeURIComponent(plate)}`)
      .then((data) => { if (active) setResult({ plate, data, error: null }) })
      .catch((error) => { if (active) setResult({ plate, data: null, error }) })
    return () => { active = false }
  }, [plate])

  if (!result || result.plate !== plate) return <p>Loading...</p>

  // Vehicle not found: offer to register it
  if (result.error) {
    if (result.error.status === 404) {
      const cleanPlate = result.error.data?.numberPlate || plate
      return (
        <>
          <h1>Vehicle not found</h1>
          <div className="card">
            <p>No vehicle with number plate <strong>{cleanPlate}</strong> is registered yet.</p>
            <div className="actions">
              <Link className="btn" to={`/vehicles/new?plate=${encodeURIComponent(cleanPlate)}`}>
                Register this vehicle
              </Link>
              <Link className="btn btn-secondary" to="/">Search again</Link>
            </div>
          </div>
        </>
      )
    }
    return <div className="alert alert-error">{result.error.message}</div>
  }

  const { vehicle, repairs, totalRepairs } = result.data
  const repeatCount = repairs.filter((r) => r.isRepeatProblem).length

  return (
    <>
      {/* Vehicle details */}
      <div className="card">
        <div className="repair-header">
          <div>
            <h1 style={{ marginBottom: 4 }}>{vehicle.numberPlate}</h1>
            <div className="muted">{vehicle.make} {vehicle.model}</div>
          </div>
          <Link className="btn" to={`/repairs/new?plate=${encodeURIComponent(vehicle.numberPlate)}`}>
            Record repair
          </Link>
        </div>
        <div className="grid-2">
          <div><strong>Owner:</strong> {vehicle.ownerName}</div>
          <div><strong>Owner phone:</strong> {vehicle.ownerPhone}</div>
          <div><strong>Total repairs:</strong> {totalRepairs}</div>
          <div><strong>Repeat problems:</strong> {repeatCount}</div>
        </div>
      </div>

      {/* Repair history from ALL garages */}
      <h2>Repair history (all garages)</h2>

      {repairs.length === 0 && (
        <div className="card muted">No repairs recorded yet for this vehicle.</div>
      )}

      {repairs.map((r) => (
        <div key={r.repairId} className={`card repair ${r.isRepeatProblem ? 'is-repeat' : ''}`}>
          <div className="repair-header">
            <div>
              <strong>{r.repairDate}</strong> · {r.garageName} ({r.province})
            </div>
            {r.isRepeatProblem && <span className="badge badge-repeat">Repeat Problem</span>}
          </div>

          <p><strong>Problem:</strong> {r.problemDescription}</p>
          <p><strong>Work done:</strong> {r.workDone}</p>
          <p>
            <strong>Repair cost:</strong> {money(r.cost)}{' '}
            <span className="muted">· Recorded by {r.recordedBy}</span>
          </p>

          {r.parts.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Part</th><th>Qty</th><th>Cost</th></tr>
                </thead>
                <tbody>
                  {r.parts.map((p) => (
                    <tr key={p.partId}>
                      <td>{p.partName}</td>
                      <td>{p.quantity}</td>
                      <td>{money(p.cost)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ))}
    </>
  )
}