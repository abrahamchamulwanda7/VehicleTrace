// Sends a request to the Java backend and returns the JSON response.
// Throws an Error with the backend's message if something goes wrong.
export async function api(path, { method = 'GET', body } = {}) {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'include', // send the login cookie
  })

  // Read the response (some responses may be empty or plain text)
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!response.ok) {
    const error = new Error(data?.error || data?.message || `Request failed (${response.status})`)
    error.status = response.status
    error.data = data
    throw error
  }

  return data
}