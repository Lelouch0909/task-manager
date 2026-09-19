import assert from 'node:assert/strict'
import { loadSource } from './load-source.mjs'

const base = process.env.API_TEST_BASE
assert.ok(base, 'API_TEST_BASE is required')
const { apiFetch, ApiError } = await loadSource('../src/lib/api.ts', { env: { VITE_API_BASE_URL: base } })
const { taskSchema, taskPageSchema, loginSchema } = await loadSource('../src/lib/contracts.ts')
const nativeFetch = globalThis.fetch
let cookie = ''
// Node has no browser cookie jar. Preserve Set-Cookie exactly as the browser would.
globalThis.fetch = async (url, options = {}) => {
  const headers = new Headers(options.headers)
  if (cookie) headers.set('Cookie', cookie)
  headers.set('Origin', 'http://localhost:5173')
  const response = await nativeFetch(url, { ...options, headers })
  const updated = response.headers.get('set-cookie')
  if (updated) cookie = updated.split(';')[0]
  return response
}
const json = body => ({ headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
try {
  let session = loginSchema.parse(await apiFetch('/auth/login', { method: 'POST', credentials: 'include',
    ...json({ email: process.env.API_TEST_EMAIL, password: process.env.API_TEST_PASSWORD }) }))
  assert.ok(cookie.startsWith('refresh_token='))
  let token = session.accessToken
  const task = taskSchema.parse(await apiFetch('/tasks', { method: 'POST', token, ...json({ title: 'Web contract', description: 'Depuis Fetch', status: 'TODO' }) }))
  const page = taskPageSchema.parse(await apiFetch('/tasks?search=Web%20contract&size=12', { token }))
  assert.ok(page.items.some(item => item.id === task.id))
  const changed = taskSchema.parse(await apiFetch('/tasks/' + task.id, { method: 'PUT', token, ...json({ title: 'Web contract', description: null, status: 'DONE' }) }))
  assert.equal(changed.status, 'DONE')
  const notifications = await apiFetch('/notifications', { token })
  assert.ok(notifications.unreadCount >= 2)
  await apiFetch('/notifications/' + notifications.items[0].id + '/read', { method: 'PUT', token, ...json({ read: true }) })
  await apiFetch('/notifications/read-all', { method: 'PUT', token })
  assert.equal((await apiFetch('/notifications/unread-count', { token })).unreadCount, 0)
  session = loginSchema.parse(await apiFetch('/auth/refresh', { method: 'POST', credentials: 'include' }))
  token = session.accessToken
  await apiFetch('/tasks/' + task.id, { method: 'DELETE', token })
  await apiFetch('/auth/logout', { method: 'POST', credentials: 'include' })
  await assert.rejects(() => apiFetch('/tasks', { token }), e => e instanceof ApiError && e.status === 401)
  console.log('Web Fetch + Zod: login, CRUD, notifications, refresh and logout passed against Spring Boot.')
} finally { globalThis.fetch = nativeFetch }
