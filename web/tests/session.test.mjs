import { test } from 'node:test'
import assert from 'node:assert/strict'
import { loadSource } from './load-source.mjs'

test('session renewal is single-flight, authenticated calls retry, logout clears memory', { concurrency: false }, async () => {
  const originalFetch = globalThis.fetch
  const OriginalChannel = globalThis.BroadcastChannel
  const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, 'navigator')
  const actions = []
  let refreshes = 0
  let requests = 0
  let locked = Promise.resolve()
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: {
    locks: { request: (_name, fn) => { const current = locked.then(fn); locked = current.catch(() => {}); return current } },
  } })
  globalThis.BroadcastChannel = class { addEventListener() {} postMessage() {} }
  globalThis.__sessionTestStore = { dispatch: action => actions.push(action) }
  const session = n => ({ accessToken: 'token-' + n, expiresIn: 900, user: {
    id: 'alice', displayName: 'Alice', email: 'alice@example.com', emailVerified: true, createdAt: '2026-01-01',
  } })
  globalThis.fetch = async (url, init) => {
    assert.equal(init.credentials, url.includes('/auth/') ? 'include' : undefined)
    if (url.endsWith('/auth/refresh')) {
      refreshes++
      await new Promise(resolve => setTimeout(resolve, 10))
      return Response.json(session(refreshes))
    }
    if (url.endsWith('/auth/logout')) return new Response(null, { status: 204 })
    requests++
    if (requests === 1) return Response.json({ code: 'unauthorized' }, { status: 401 })
    assert.equal(init.headers.get('Authorization'), 'Bearer token-2')
    return Response.json({ ok: true })
  }
  try {
    const api = await loadSource('../src/features/auth/session.ts', {
      env: { VITE_API_BASE_URL: 'http://test/api' },
      mocks: { '@/app/store': 'export const store = globalThis.__sessionTestStore' },
    })
    const tokens = await Promise.all([api.getToken(), api.getToken(), api.getToken()])
    assert.deepEqual(tokens, ['token-1', 'token-1', 'token-1'])
    assert.equal(refreshes, 1)
    assert.deepEqual(await api.authRequest('/tasks'), { ok: true })
    assert.equal(refreshes, 2)
    await api.logout()
    assert.ok(actions.some(action => action.type === 'auth/signedOut'))
    await api.getToken()
    assert.equal(refreshes, 3, 'logout must discard the previous access token')
  } finally {
    globalThis.fetch = originalFetch
    globalThis.BroadcastChannel = OriginalChannel
    if (originalNavigator) Object.defineProperty(globalThis, 'navigator', originalNavigator)
    delete globalThis.__sessionTestStore
  }
})

test('invalid refresh makes the session anonymous without retries', { concurrency: false }, async () => {
  const originalFetch = globalThis.fetch
  const OriginalChannel = globalThis.BroadcastChannel
  const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, 'navigator')
  const actions = []
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { locks: { request: (_name, fn) => fn() } } })
  globalThis.BroadcastChannel = class { addEventListener() {} postMessage() {} }
  globalThis.__sessionTestStore = { dispatch: action => actions.push(action) }
  globalThis.fetch = async () => Response.json({ code: 'invalid_session' }, { status: 401 })
  try {
    const api = await loadSource('../src/features/auth/session.ts', {
      env: { VITE_API_BASE_URL: 'http://test/api' },
      mocks: { '@/app/store': 'export const store = globalThis.__sessionTestStore' },
    })
    assert.equal(await api.getToken(), null)
    assert.ok(actions.some(action => action.type === 'auth/signedOut'))
  } finally {
    globalThis.fetch = originalFetch; globalThis.BroadcastChannel = OriginalChannel
    if (originalNavigator) Object.defineProperty(globalThis, 'navigator', originalNavigator)
    delete globalThis.__sessionTestStore
  }
})
