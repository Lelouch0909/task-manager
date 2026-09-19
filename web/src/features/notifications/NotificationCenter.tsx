import { useEffect, useRef, useState } from 'react'
import { Bell, CheckCheck, ArrowUpRight, CircleCheck, Circle, ChevronLeft, ChevronRight, RotateCcw } from 'lucide-react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetTrigger } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { startNotificationClient } from './notificationClient'
import { getToken } from '@/features/auth/session'
import { errorMessage } from '@/lib/errors'
import { cn } from '@/lib/utils'

export function NotificationCenter() {
  const dispatch = useAppDispatch()
  const { page, error, connection } = useAppSelector(state => state.notifications)
  const client = useRef<ReturnType<typeof startNotificationClient> | null>(null)
  const [pending, setPending] = useState(false)
  const [unread, setUnread] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  useEffect(() => {
    const instance = startNotificationClient({ dispatch, getToken })
    client.current = instance
    return () => { instance.stop(); client.current = null }
  }, [dispatch])
  async function update(id?: string, read?: boolean) {
    if (!client.current) return
    setPending(true); setActionError(null)
    try {
      if (id) await client.current.setRead(id, Boolean(read))
      else await client.current.readAll()
    } catch (e) { setActionError(errorMessage(e)) }
    finally { setPending(false) }
  }
  return <Sheet>
    <SheetTrigger asChild><Button variant="ghost" size="icon" className="relative rounded-full" aria-label={`Notifications, ${page?.unreadCount ?? 0} non lues`}><Bell className="size-5" />{Boolean(page?.unreadCount) && <span className="absolute right-1.5 top-1.5 size-2 rounded-full bg-coral ring-2 ring-white" />}</Button></SheetTrigger>
    <SheetContent className="w-full gap-0 sm:max-w-[420px]">
      <SheetHeader className="border-b px-6 pb-5 pt-8">
        <div className="mb-3 flex size-11 items-center justify-center rounded-2xl bg-brand/10 text-primary"><Bell className="size-5" /></div>
        <SheetTitle className="text-2xl">Votre fil d’activité<span className="text-coral">.</span></SheetTitle>
        <SheetDescription>Chaque petit pas, au même endroit.</SheetDescription>
        <p className="mt-2 flex items-center gap-2 text-xs text-muted-foreground"><span className={cn('size-1.5 rounded-full', connection === 'connected' ? 'bg-brand' : 'bg-amber-500')} />{connection === 'connected' ? 'À jour en temps réel' : 'Reconnexion en cours…'}</p>
      </SheetHeader>
      <div className="flex items-center justify-between border-b p-4">
        <div className="flex rounded-lg bg-muted p-1">{[false, true].map(value => <Button key={String(value)} size="sm" variant="ghost" aria-pressed={unread === value} className={unread === value ? 'bg-white shadow-sm' : ''} onClick={() => { setUnread(value); void client.current?.setUnreadOnly(value) }}>{value ? `Non lues (${page?.unreadCount ?? 0})` : 'Toutes'}</Button>)}</div>
        <Button size="icon" variant="ghost" aria-label="Tout marquer comme lu" disabled={pending || !page?.unreadCount} onClick={() => void update()}><CheckCheck /></Button>
      </div>
      {(actionError || error) && <div role="alert" className="mx-4 mt-3 rounded-lg bg-red-50 p-3 text-sm text-destructive">{actionError || error}<Button variant="ghost" size="sm" onClick={() => void client.current?.reload()}><RotateCcw />Réessayer</Button></div>}
      <div className="flex-1 overflow-y-auto px-4" aria-live="polite">
        {!page && !error && <div className="space-y-3 py-5">{[1,2,3].map(n => <Skeleton key={n} className="h-20 w-full" />)}</div>}
        {page?.items.length === 0 && <div className="py-20 text-center"><CircleCheck className="mx-auto mb-4 size-10 text-brand/50" /><h3 className="font-semibold">{unread ? 'Vous êtes à jour !' : 'Le début de votre histoire'}</h3><p className="mt-2 text-sm text-muted-foreground">{unread ? 'Toutes vos notifications ont été lues.' : 'Vos prochaines actions apparaîtront ici.'}</p></div>}
        <ul className="divide-y">{page?.items.map(item => <li key={item.id} className={cn('group flex gap-3 px-2 py-5 transition-colors', !item.readAt && 'bg-brand/[.025]')}>
          <span className={cn('mt-0.5 grid size-9 shrink-0 place-items-center rounded-xl', item.kind === 'DELETED' ? 'bg-coral/10 text-coral' : 'bg-brand/10 text-primary')}><ArrowUpRight className="size-4" /></span>
          <div className="min-w-0 flex-1"><p className={cn('break-words text-sm leading-6', !item.readAt && 'font-semibold')}>{item.message}</p><time dateTime={item.createdAt} className="mt-1 block text-xs text-muted-foreground">{new Date(item.createdAt).toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</time>
            <button disabled={pending} className="mt-2 flex items-center gap-1.5 text-xs text-primary hover:underline disabled:opacity-50" onClick={() => void update(item.id, !item.readAt)}>{item.readAt ? <Circle className="size-3" /> : <CheckCheck className="size-3" />}{item.readAt ? 'Marquer non lue' : 'Marquer lue'}</button>
          </div>{!item.readAt && <span className="mt-2 size-1.5 shrink-0 rounded-full bg-coral" />}
        </li>)}</ul>
      </div>
      {page && page.totalPages > 1 && <div className="flex items-center justify-between border-t p-4"><Button variant="outline" size="icon" aria-label="Page précédente de notifications" disabled={page.page === 0} onClick={() => void client.current?.setPage(page.page - 1)}><ChevronLeft /></Button><span className="text-xs text-muted-foreground">{page.page + 1} / {page.totalPages}</span><Button variant="outline" size="icon" aria-label="Page suivante de notifications" disabled={page.page + 1 >= page.totalPages} onClick={() => void client.current?.setPage(page.page + 1)}><ChevronRight /></Button></div>}
    </SheetContent>
  </Sheet>
}
