import type { AppDispatch } from '@/app/store'
import { apiFetch, API_BASE_URL, ApiError } from '@/lib/api'
import { readSse } from '@/lib/sse'
import { received, failed, connectionChanged, tasksInvalidated, cleared, type NotificationPage } from './notificationsSlice'
import { errorMessage } from '@/lib/errors'

function parsePage(value: unknown): NotificationPage {
  const page = value as NotificationPage
  if (!page || !Array.isArray(page.items) ||
      !['page', 'size', 'totalElements', 'totalPages', 'unreadCount'].every(key =>
        typeof (page as unknown as Record<string, unknown>)[key] === 'number') ||
      !page.items.every(item => item && typeof item.id === 'string' && typeof item.taskId === 'string' &&
        typeof item.kind === 'string' && typeof item.message === 'string' && typeof item.createdAt === 'string' &&
        (item.readAt === null || typeof item.readAt === 'string'))) throw new Error('Réponse de notifications invalide.')
  return page
}

/** Mount once per authenticated app, and call stop on logout/unmount.
 * getToken must use the central auth refresh mechanism (including cross-tab serialization).
 */
export function startNotificationClient(options: {
  dispatch: AppDispatch
  getToken: (renew?: boolean) => Promise<string | null>
  onTasksChanged?: () => void
}) {
  const { dispatch, getToken, onTasksChanged } = options
  const controller = new AbortController()
  const { signal } = controller
  let stopped = false
  let loading = false
  let dirty = false
  let retry = 1000
  let pageNumber = 0
  let unreadOnly = false
  let timer: ReturnType<typeof setTimeout> | undefined

  async function request(path: string, init: RequestInit = {}) {
    let token = await getToken()
    if (!token) throw new Error('Connexion requise.')
    try { return await apiFetch(path, { ...init, token, signal }) }
    catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error
      token = await getToken(true)
      if (!token) throw new Error('Session expirée.')
      return apiFetch(path, { ...init, token, signal })
    }
  }
  async function reload() {
    dirty = true
    if (loading || stopped) return
    loading = true
    try {
      while (dirty && !stopped) {
        dirty = false
        const requestedPage = pageNumber
        const requestedFilter = unreadOnly
        const page = parsePage(await request(`/notifications?page=${pageNumber}&unreadOnly=${unreadOnly}`))
        if (requestedPage !== pageNumber || requestedFilter !== unreadOnly) { dirty = true; continue }
        if (pageNumber > 0 && pageNumber >= page.totalPages) { pageNumber = Math.max(0, page.totalPages - 1); dirty = true; continue }
        if (!stopped) dispatch(received(page))
      }
    } catch (error) {
      if (!stopped) dispatch(failed(errorMessage(error)))
    } finally { loading = false }
  }
  async function connect() {
    if (stopped) return
    dispatch(connectionChanged('connecting'))
    try {
      let token = await getToken()
      if (!token || stopped) return
      const open = (accessToken: string) => fetch(`${API_BASE_URL}/events`, {
        headers: { Authorization: `Bearer ${accessToken}`, Accept: 'text/event-stream' }, signal,
      })
      let response = await open(token)
      if (response.status === 401) {
        token = await getToken(true)
        if (!token || stopped) return
        response = await open(token)
      }
      if (!response.ok) throw new Error(`Flux indisponible (HTTP ${response.status}).`)
      if (!response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('Flux inattendu.')
      dispatch(connectionChanged('connected'))
      await readSse(response, (event, data) => {
        if (stopped || event !== 'sync') return
        const update = JSON.parse(data) as { tasks?: boolean; notifications?: boolean }
        if (typeof update.tasks !== 'boolean' || typeof update.notifications !== 'boolean') return
        retry = 1000
        if (update.tasks) { dispatch(tasksInvalidated()); onTasksChanged?.() }
        if (update.notifications) void reload()
      })
    } catch (error) {
      if (!stopped) dispatch(failed(error instanceof Error ? error.message : 'Connexion interrompue.'))
    } finally {
      if (!stopped) {
        dispatch(connectionChanged('disconnected'))
        timer = setTimeout(() => { void reload(); void connect() }, retry)
        retry = Math.min(retry * 2, 30000)
      }
    }
  }
  void reload()
  void connect()
  return {
    setPage(page: number) { pageNumber = Math.max(0, page); return reload() },
    setUnreadOnly(value: boolean) { unreadOnly = value; pageNumber = 0; return reload() },
    reload,
    async setRead(id: string, read: boolean) {
      await request(`/notifications/${encodeURIComponent(id)}/read`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ read }),
      })
      await reload()
    },
    async readAll() { await request('/notifications/read-all', { method: 'PUT' }); await reload() },
    stop() {
      stopped = true; controller.abort(); clearTimeout(timer); dispatch(cleared())
    },
  }
}
