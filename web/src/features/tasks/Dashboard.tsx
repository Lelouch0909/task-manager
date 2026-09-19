import { useState, useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { ArrowUpRight, Plus, Search, LayoutGrid, List, ListTodo, CircleCheck, CircleDashed, ArrowRight, LogOut, Menu, ChevronLeft, ChevronRight, X, LoaderCircle, Sparkles, RotateCcw, AlertCircle, Leaf } from 'lucide-react'
import { Brand } from '@/components/Brand'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Progress } from '@/components/ui/progress'
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator } from '@/components/ui/dropdown-menu'
import { AlertDialog, AlertDialogContent, AlertDialogHeader, AlertDialogTitle, AlertDialogDescription, AlertDialogFooter, AlertDialogCancel } from '@/components/ui/alert-dialog'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { authRequest, logout } from '@/features/auth/session'
import { NotificationCenter } from '@/features/notifications/NotificationCenter'
import { TaskEditor } from './TaskEditor'
import { TaskCard } from './TaskCard'
import { useTasks } from './useTasks'
import { filterChanged, searchChanged, pageChanged } from './tasksSlice'
import { setViewMode } from '@/features/ui/uiSlice'
import { type Task, type TaskStatus, taskSchema } from '@/lib/contracts'
import { errorMessage } from '@/lib/errors'
import { cn } from '@/lib/utils'

export function Dashboard() {
  const dispatch = useAppDispatch()
  const user = useAppSelector(s => s.auth.user)!
  const view = useAppSelector(s => s.ui.viewMode)
  const { data, counts, loading, error, reload, filter, search, page } = useTasks()
  const [editor, setEditor] = useState<Task | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Task | null>(null)
  const [pending, setPending] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [signingOut, setSigningOut] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  const total = counts ? counts.TODO + counts.IN_PROGRESS + counts.DONE : 0
  const percent = total && counts ? Math.round(counts.DONE / total * 100) : 0
  const initials = user.displayName.split(' ').map(n => n[0]).join('').slice(0,2).toUpperCase()
  useEffect(() => {
    function shortcut(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') { e.preventDefault(); searchRef.current?.focus() }
    }
    window.addEventListener('keydown', shortcut)
    return () => window.removeEventListener('keydown', shortcut)
  }, [])
  async function signOut() {
    setSigningOut(true)
    try { await logout() } catch (e) { toast.error(errorMessage(e)); setSigningOut(false) }
  }
  async function changeStatus(task: Task, status: TaskStatus) {
    setPending(task.id)
    try {
      taskSchema.parse(await authRequest('/tasks/' + task.id, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ title: task.title, description: task.description, status }) }))
      toast.success(status === 'DONE' ? 'Bien joué. Une petite victoire de plus !' : 'La tâche a changé de cap.')
      reload()
    } catch (e) { toast.error(errorMessage(e)) }
    finally { setPending(null) }
  }
  async function remove() {
    if (!deleting) return
    setPending(deleting.id); setDeleteError('')
    try {
      await authRequest('/tasks/' + deleting.id, { method: 'DELETE' })
      toast.success('Tâche supprimée. Place à la suite.'); setDeleting(null); reload()
    } catch (e) { setDeleteError(errorMessage(e)) }
    finally { setPending(null) }
  }
  const nav = [
    { id: 'ALL', label: 'Toutes les tâches', icon: LayoutGrid, count: total },
    { id: 'TODO', label: 'À faire', icon: CircleDashed, count: counts?.TODO },
    { id: 'IN_PROGRESS', label: 'En cours', icon: ArrowUpRight, count: counts?.IN_PROGRESS },
    { id: 'DONE', label: 'Terminées', icon: CircleCheck, count: counts?.DONE },
  ] as const
  function sidebar() {
    return <div className="flex h-full flex-col px-6 py-9">
      <Brand />
      <div className="mb-5 mt-14 flex items-center gap-2 text-xs font-medium text-muted-foreground"><span className="size-2 rounded-full bg-brand" /> Mon espace personnel</div>
      <nav aria-label="Navigation principale" className="space-y-2">{nav.map(item => <button key={item.id} onClick={() => { dispatch(filterChanged(item.id)); setMobileOpen(false) }} aria-current={filter === item.id ? 'page' : undefined} className={cn('flex w-full items-center gap-3 rounded-xl px-3.5 py-3 text-sm font-medium transition-all', filter === item.id ? 'bg-[#e6f3ee] text-[#17675d]' : 'text-muted-foreground hover:bg-muted hover:text-foreground')}><item.icon className="size-[18px]" /><span>{item.label}</span><span className={cn('ml-auto min-w-6 rounded-md px-1.5 py-0.5 text-center text-[11px]', filter === item.id ? 'bg-white/80' : 'bg-muted')}>{counts ? item.count : '–'}</span></button>)}</nav>
      <div className="mt-auto pt-10">
        <div className="relative overflow-hidden rounded-2xl bg-[#f2f6ef] p-5">
          <Leaf className="mb-4 size-6 text-[#739975]" />
          <p className="font-display text-sm font-bold">À votre rythme.</p><p className="mb-4 mt-2 text-xs leading-5 text-muted-foreground">Les grandes choses commencent par de petites actions.</p>
          <button className="flex items-center gap-2 text-xs font-semibold text-[#497450] hover:gap-3 transition-all" onClick={() => setEditor('new')}>Faire un premier pas <ArrowRight className="size-3" /></button>
        </div>
        <div className="mt-7 border-t pt-5 text-[10px] tracking-wide text-muted-foreground">MOINS DE BRUIT. PLUS DE CLARTÉ.</div>
      </div>
    </div>
  }
  return <div className="min-h-svh">
    <a href="#main" className="sr-only z-50 bg-white p-3 focus:not-sr-only focus:absolute">Aller aux tâches</a>
    <aside className="fixed inset-y-0 left-0 z-20 hidden w-[260px] border-r bg-white lg:block">{sidebar()}</aside>
    <div className="lg:ml-[260px]">
      <header className="flex h-20 items-center justify-between border-b bg-white/90 px-5 backdrop-blur-md sm:px-9 xl:px-12">
        <div className="flex items-center gap-3">
          <Sheet open={mobileOpen} onOpenChange={setMobileOpen}><SheetTrigger asChild><Button aria-label="Ouvrir la navigation" variant="ghost" size="icon" className="lg:hidden"><Menu /></Button></SheetTrigger><SheetContent side="left" className="w-[290px] p-0"><SheetTitle className="sr-only">Navigation</SheetTitle>{sidebar()}</SheetContent></Sheet>
          <span className="text-sm text-muted-foreground">Mon espace <span className="mx-3 text-border">/</span><span className="font-medium text-foreground">Mes tâches</span></span>
        </div>
        <div className="flex items-center gap-3"><span className="mr-2 hidden text-xs capitalize text-muted-foreground md:block">{new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}</span><NotificationCenter /><span className="mx-1 h-6 w-px bg-border" />
          <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className="h-auto rounded-full p-0" aria-label="Mon compte"><Avatar className="size-9"><AvatarFallback className="bg-[#f5dfd6] text-xs font-bold text-[#915748]">{initials}</AvatarFallback></Avatar></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="w-64"><DropdownMenuLabel className="break-words">{user.displayName}<p className="mt-1 text-xs font-normal text-muted-foreground">{user.email}</p></DropdownMenuLabel><DropdownMenuSeparator /><DropdownMenuItem disabled={signingOut} onSelect={() => void signOut()}><LogOut />Se déconnecter</DropdownMenuItem></DropdownMenuContent></DropdownMenu>
        </div>
      </header>
      <main id="main" className="page-enter mx-auto max-w-[1500px] px-5 py-9 sm:px-9 xl:px-12">
        <div className="mb-8 flex flex-wrap items-center justify-between gap-5">
          <div><p className="muted-label mb-3">UN NOUVEAU JOUR, DE NOUVELLES POSSIBILITÉS</p><h1 className="text-[30px] font-bold tracking-[-.04em] sm:text-4xl">Bonjour, {user.displayName.split(' ')[0]}<span className="text-coral">.</span> <span className="ml-1 inline-block text-2xl" aria-hidden="true">☀</span></h1><p className="mt-3 text-sm text-muted-foreground">On fait de la place à ce qui compte ?</p></div>
          <Button onClick={() => setEditor('new')} className="h-11 rounded-xl bg-[#163f39] px-5 shadow-sm hover:bg-[#24564e]"><Plus className="size-4" /> Nouvelle tâche</Button>
        </div>
        <section aria-label="Vue d’ensemble" className="mb-9 grid gap-4 sm:grid-cols-3">
          <div className="relative overflow-hidden rounded-2xl bg-[#123f39] p-6 text-white">
            <div className="relative z-10"><div className="flex items-center justify-between"><span className="text-xs text-white/65">Votre progression</span><Sparkles className="size-4 text-[#9fd8c8]" /></div><div className="mb-4 mt-4 flex items-end gap-2"><span className="font-display text-4xl font-semibold tracking-tight">{counts ? percent : '–'}<span className="text-xl text-white/60">%</span></span><span className="mb-1.5 text-xs text-white/60">du chemin parcouru</span></div><Progress value={percent} className="h-1.5 bg-white/10 [&>div]:bg-[#94d7c4]" /><p className="mt-3 text-[11px] text-white/60">{counts ? `${counts.DONE} tâche${counts.DONE > 1 ? 's' : ''} terminée${counts.DONE > 1 ? 's' : ''} sur ${total}` : 'Chargement de votre progression…'}</p></div><div className="absolute -bottom-14 -right-12 size-40 rounded-full border-[22px] border-white/[.035]" />
          </div>
          <button onClick={() => dispatch(filterChanged('IN_PROGRESS'))} className="group rounded-2xl border bg-white p-6 text-left transition hover:border-[#ead3af] hover:shadow-sm"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">En mouvement</span><span className="grid size-8 place-items-center rounded-xl bg-[#fff1dc] text-[#ac792b]"><ArrowUpRight className="size-4 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5" /></span></div><p className="mt-3 font-display text-4xl font-semibold tracking-tight">{counts?.IN_PROGRESS ?? '–'}</p><p className="mt-3 text-xs text-muted-foreground">Tâches en cours <span className="ml-1 text-[#b1803f]">· Continuez sur votre lancée</span></p></button>
          <button onClick={() => dispatch(filterChanged('TODO'))} className="group rounded-2xl border bg-white p-6 text-left transition hover:border-coral/30 hover:shadow-sm"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">À l’horizon</span><span className="grid size-8 place-items-center rounded-xl bg-coral/10 text-coral"><ListTodo className="size-4 transition group-hover:rotate-6" /></span></div><p className="mt-3 font-display text-4xl font-semibold tracking-tight">{counts?.TODO ?? '–'}</p><p className="mt-3 text-xs text-muted-foreground">Tâches à faire <span className="ml-1 text-coral">· Chaque chose en son temps</span></p></button>
        </section>
        <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3"><h2 className="text-xl font-bold tracking-tight">Mes tâches</h2><span className="rounded-md bg-[#eaf0ed] px-2 py-0.5 text-xs text-muted-foreground">{data?.totalElements ?? '–'}</span></div>
          <div className="flex items-center gap-1 rounded-lg border bg-white p-1" role="group" aria-label="Affichage">{(['grid','list'] as const).map(mode => <Button key={mode} aria-label={mode === 'grid' ? 'Vue cartes' : 'Vue liste'} aria-pressed={view === mode} variant="ghost" size="icon-sm" className={view === mode ? 'bg-[#eaf3ef] text-primary' : 'text-muted-foreground'} onClick={() => dispatch(setViewMode(mode))}>{mode === 'grid' ? <LayoutGrid className="size-4" /> : <List className="size-4" />}</Button>)}</div>
        </div>
        <div className="mb-6 flex flex-col justify-between gap-4 border-b pb-4 xl:flex-row">
          <div className="flex flex-wrap gap-1" role="group" aria-label="Filtrer les tâches">{nav.map(item => <Button key={item.id} size="sm" variant="ghost" aria-pressed={filter === item.id} className={cn('rounded-lg text-xs', filter === item.id ? 'bg-[#e6f1ec] text-[#276457]' : 'text-muted-foreground')} onClick={() => dispatch(filterChanged(item.id))}>{item.id === 'ALL' ? 'Toutes' : item.label}<span className="ml-1 opacity-60">{counts ? item.count : ''}</span></Button>)}</div>
          <div className="relative w-full xl:w-64"><Search className="pointer-events-none absolute left-3 top-2.5 size-4 text-muted-foreground" /><Input ref={searchRef} aria-label="Rechercher une tâche" placeholder="Rechercher une tâche…" className="h-9 bg-white pl-9 pr-9 text-xs" value={search} maxLength={200} onChange={e => dispatch(searchChanged(e.target.value))} />{search ? <button aria-label="Effacer la recherche" className="absolute right-3 top-2.5" onClick={() => dispatch(searchChanged(''))}><X className="size-4 text-muted-foreground" /></button> : <kbd className="pointer-events-none absolute right-2 top-2 rounded border px-1 text-[10px] text-muted-foreground">⌘ K</kbd>}</div>
        </div>
        {error ? <div role="alert" className="rounded-2xl border border-red-100 bg-white p-8 text-center"><AlertCircle className="mx-auto mb-3 text-coral" /><p className="text-sm">{error}</p><Button className="mt-4" variant="outline" onClick={reload}><RotateCcw />Réessayer</Button></div> :
          loading ? <div aria-label="Chargement des tâches" className={cn('grid gap-4', view === 'grid' && 'sm:grid-cols-2 xl:grid-cols-3')}>{[1,2,3,4,5,6].map(n => <Skeleton key={n} className={cn('rounded-2xl bg-[#eaf0ed]', view === 'grid' ? 'h-56' : 'h-24')} />)}</div> :
          data?.items.length ? <div className={cn('grid gap-4', view === 'grid' && 'sm:grid-cols-2 xl:grid-cols-3')}>{data.items.map(task => <TaskCard key={task.id} task={task} grid={view === 'grid'} busy={pending !== null} onEdit={() => setEditor(task)} onDelete={() => { setDeleteError(''); setDeleting(task) }} onStatus={status => void changeStatus(task, status)} />)}</div> :
          <div className="rounded-2xl border border-dashed bg-white/60 px-6 py-16 text-center"><div className="mx-auto mb-5 grid size-16 -rotate-6 place-items-center rounded-2xl bg-brand/10 text-primary">{search ? <Search className="size-7" /> : <LayersEmpty />}</div><h3 className="text-lg font-bold">{search ? 'Pas de résultat, pour le moment.' : filter === 'DONE' ? 'Vos petites victoires arrivent.' : 'De la place pour vos prochaines idées.'}</h3><p className="mx-auto mb-6 mt-2 max-w-sm text-sm leading-6 text-muted-foreground">{search ? 'Essayez un autre mot ou ajustez vos filtres.' : 'Une tâche, une intention. Quel sera votre prochain pas ?'}</p><Button variant="outline" onClick={() => { if (search) dispatch(searchChanged('')); else setEditor('new') }}>{search ? 'Effacer la recherche' : <><Plus />Créer une tâche</>}</Button></div>}
        {data && data.totalPages > 1 && <div className="mt-6 flex items-center justify-between"><span className="text-xs text-muted-foreground">Page {page + 1} sur {data.totalPages} · {data.totalElements} tâches</span><div className="flex gap-2"><Button variant="outline" size="icon" disabled={page === 0 || loading} aria-label="Page précédente" onClick={() => dispatch(pageChanged(page - 1))}><ChevronLeft /></Button><Button variant="outline" size="icon" disabled={page + 1 >= data.totalPages || loading} aria-label="Page suivante" onClick={() => dispatch(pageChanged(page + 1))}><ChevronRight /></Button></div></div>}
        <footer className="mt-10 flex items-center justify-between border-t pt-5 text-[11px] text-muted-foreground"><span>Chaque petite action compte.</span><span className="hidden items-center gap-1.5 sm:flex"><span className="size-1 rounded-full bg-brand" />Votre espace, votre rythme.</span></footer>
      </main>
    </div>
    {editor && <TaskEditor task={editor === 'new' ? undefined : editor} onClose={() => setEditor(null)} onSaved={reload} />}
    <AlertDialog open={Boolean(deleting)} onOpenChange={open => { if (!open && !pending) setDeleting(null) }}><AlertDialogContent className="rounded-2xl"><AlertDialogHeader><AlertDialogTitle>Faire de la place ?</AlertDialogTitle><AlertDialogDescription>La tâche « {deleting?.title} » sera définitivement supprimée. Cette action ne peut pas être annulée.</AlertDialogDescription></AlertDialogHeader>{deleteError && <p role="alert" className="text-sm text-destructive">{deleteError}</p>}<AlertDialogFooter><AlertDialogCancel disabled={Boolean(pending)}>Garder la tâche</AlertDialogCancel><Button variant="destructive" disabled={Boolean(pending)} onClick={() => void remove()}>{pending && <LoaderCircle className="animate-spin" />}Supprimer la tâche</Button></AlertDialogFooter></AlertDialogContent></AlertDialog>
  </div>
}
function LayersEmpty() { return <ListTodo className="size-7" /> }
