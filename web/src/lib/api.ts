export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number
  readonly details: unknown

  constructor(status: number, details: unknown) {
    super(`La requête a échoué (HTTP ${status}).`)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

type ApiOptions = RequestInit & { token?: string }

// Returns unknown: each feature must validate its API response before using it.
export async function apiFetch(path: string, options: ApiOptions = {}): Promise<unknown> {
  const { token, ...init } = options
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_BASE_URL}/${path.replace(/^\//, '')}`, {
    ...init,
    headers,
  })
  const text = await response.text()
  let data: unknown = text || undefined
  if (text && response.headers.get('content-type')?.includes('json')) {
    try {
      data = JSON.parse(text)
    } catch {
      if (response.ok) throw new Error('La réponse JSON de l’API est invalide.')
    }
  }
  if (!response.ok) throw new ApiError(response.status, data)
  return data
}
