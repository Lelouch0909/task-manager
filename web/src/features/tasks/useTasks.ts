import { useEffect, useState, useCallback } from 'react'
import { authRequest } from '@/features/auth/session'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { taskPageSchema, type TaskPage, type TaskStatus } from '@/lib/contracts'
import { errorMessage } from '@/lib/errors'
import { pageChanged } from './tasksSlice'

export function useTasks() {
  const dispatch = useAppDispatch()
  const { filter, search, page } = useAppSelector(s => s.tasks)
  const revision = useAppSelector(s => s.notifications.tasksRevision)
  const [query, setQuery] = useState(search)
  const [data, setData] = useState<TaskPage | null>(null)
  const [counts, setCounts] = useState<Record<TaskStatus, number> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)
  const reload = useCallback(() => setVersion(n => n + 1), [])
  useEffect(() => { const timer = setTimeout(() => setQuery(search), 300); return () => clearTimeout(timer) }, [search])
  useEffect(() => {
    const abort = new AbortController()
    // These states represent a new external request, not values derived from props.
    // oxlint-disable-next-line react/set-state-in-effect
    setLoading(true); setError('')
    const params = new URLSearchParams({ page: String(page), size: '12' })
    if (filter !== 'ALL') params.set('status', filter)
    if (query.trim()) params.set('search', query.trim())
    authRequest('/tasks?' + params, { signal: abort.signal })
      .then(taskPageSchema.parse)
      .then(result => {
        if (abort.signal.aborted) return
        if (page > 0 && page >= result.totalPages) dispatch(pageChanged(Math.max(0, result.totalPages - 1)))
        else setData(result)
      }).catch(e => { if (!abort.signal.aborted) setError(errorMessage(e)) })
      .finally(() => { if (!abort.signal.aborted) setLoading(false) })
    return () => abort.abort()
  }, [filter, query, page, revision, version, dispatch])
  useEffect(() => {
    const abort = new AbortController()
    const statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE']
    Promise.all(statuses.map(status => authRequest('/tasks?size=1&status=' + status, { signal: abort.signal }).then(taskPageSchema.parse)))
      .then(pages => { if (!abort.signal.aborted) setCounts({ TODO: pages[0].totalElements, IN_PROGRESS: pages[1].totalElements, DONE: pages[2].totalElements }) })
      .catch(() => { if (!abort.signal.aborted) setCounts(null) })
    return () => abort.abort()
  }, [revision, version])
  return { data, counts, loading, error, reload, filter, search, page }
}
