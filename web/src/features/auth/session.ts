import { apiFetch, ApiError } from '@/lib/api'
import { loginSchema } from '@/lib/contracts'
import { errorMessage } from '@/lib/errors'
import { store } from '@/app/store'
import { signedIn, signedOut, authFailed } from './authSlice'
import { cleared } from '@/features/notifications/notificationsSlice'

let token: string | null = null
let expiresAt = 0
let generation = 0
let refreshFlight: Promise<string | null> | null = null
let bootstrap: Promise<void> | null = null
const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('taskmanager-session')
function clear() {
  generation++; token = null; expiresAt = 0
  store.dispatch(signedOut()); store.dispatch(cleared())
}
channel?.addEventListener('message', event => {
  if (event.data === 'logout') clear()
  if (event.data === 'login') { clear(); void getToken(true).catch(() => {}) }
})
async function sessionLock<T>(work: () => Promise<T>): Promise<T> {
  if (!navigator.locks) throw new Error('Ce navigateur ne permet pas de sécuriser le renouvellement de session. Utilisez une version récente de votre navigateur.')
  return navigator.locks.request('taskmanager-refresh', work)
}
function accept(data: unknown) {
  const session = loginSchema.parse(data)
  token = session.accessToken; expiresAt = Date.now() + session.expiresIn * 1000
  store.dispatch(signedIn(session.user))
  return token
}
export async function getToken(renew = false): Promise<string | null> {
  if (!renew && token && expiresAt > Date.now() + 30000) return token
  if (refreshFlight) return refreshFlight
  const epoch = generation
  refreshFlight = sessionLock(async () => {
    if (epoch !== generation) return null
    try {
      const data = await apiFetch('/auth/refresh', { method: 'POST', credentials: 'include' })
      return epoch === generation ? accept(data) : null
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) { if (epoch === generation) clear(); return null }
      throw error
    }
  }).finally(() => { refreshFlight = null })
  return refreshFlight
}
export function restoreSession() {
  bootstrap ??= getToken().then(() => {}).catch(error => { store.dispatch(authFailed(errorMessage(error))) })
  return bootstrap
}
export async function login(email: string, password: string) {
  await sessionLock(async () => {
    const data = await apiFetch('/auth/login', { method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
    generation++; accept(data); channel?.postMessage('login')
  })
}
export async function logout() {
  await sessionLock(async () => {
    await apiFetch('/auth/logout', { method: 'POST', credentials: 'include' })
    clear(); channel?.postMessage('logout')
  })
}
export async function authRequest(path: string, init: RequestInit = {}) {
  const current = await getToken()
  if (!current) throw new Error('Votre session a expiré. Reconnectez-vous.')
  try { return await apiFetch(path, { ...init, token: current }) }
  catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error
    const fresh = token && token !== current ? token : await getToken(true)
    if (!fresh) throw new Error('Votre session a expiré. Reconnectez-vous.')
    return apiFetch(path, { ...init, token: fresh })
  }
}
