import { Check, Circle, MoreHorizontal, Pencil, Trash2, ArrowUpRight, CircleDashed, LoaderCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator, DropdownMenuLabel } from '@/components/ui/dropdown-menu'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'
import { statusLabels, type Task, type TaskStatus } from '@/lib/contracts'

export function TaskCard({ task, grid, busy, onEdit, onDelete, onStatus }: {
  task: Task; grid: boolean; busy: boolean; onEdit: () => void; onDelete: () => void; onStatus: (status: TaskStatus) => void
}) {
  const done = task.status === 'DONE'
  return <article className={cn('task-card group relative rounded-2xl border bg-white p-5', grid ? 'flex min-h-56 flex-col' : 'flex items-center gap-4 sm:p-5', done && 'bg-white/70')} aria-label={task.title} aria-busy={busy}>
    <div className={cn('flex items-center gap-3', grid ? 'mb-5 justify-between' : 'contents')}>
      <Tooltip><TooltipTrigger asChild><button disabled={busy} onClick={() => onStatus(done ? 'TODO' : 'DONE')} aria-label={`${done ? 'Réouvrir' : 'Terminer'} : ${task.title}`} className={cn('grid size-7 shrink-0 place-items-center rounded-full border-2 transition-all duration-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand disabled:opacity-50', done ? 'border-brand bg-brand text-white' : 'border-[#d5dfdb] text-transparent hover:scale-110 hover:border-brand hover:text-brand')}>{busy ? <LoaderCircle className="size-4 animate-spin text-primary" /> : <Check className="size-4" />}</button></TooltipTrigger><TooltipContent>{done ? 'Remettre à faire' : 'Une tâche de moins !'}</TooltipContent></Tooltip>
      {grid && <StatusBadge status={task.status} />}
    </div>
    <button disabled={busy} className="min-w-0 flex-1 text-left focus-visible:rounded focus-visible:outline-2 focus-visible:outline-brand" onClick={onEdit}>
      <h3 className={cn('break-words text-[15px] font-bold leading-6', done && 'text-muted-foreground line-through decoration-muted-foreground/40')}>{task.title}</h3>
      <p className={cn('mt-2 text-sm leading-6 text-muted-foreground', grid ? 'line-clamp-3' : 'line-clamp-1')}>{task.description || 'Un petit pas qui fera la différence.'}</p>
    </button>
    {!grid && <div className="hidden sm:block"><StatusBadge status={task.status} /></div>}
    <div className={cn('flex items-center justify-between', grid && 'mt-5 border-t pt-4')}>
      <time className={cn('text-[11px] text-muted-foreground', !grid && 'mr-4 hidden xl:block')} dateTime={task.createdAt}>{new Date(task.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</time>
      <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon-sm" aria-label={`Options : ${task.title}`} disabled={busy}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="w-48">
        <DropdownMenuItem onSelect={onEdit}><Pencil />Modifier</DropdownMenuItem><DropdownMenuSeparator /><DropdownMenuLabel>Déplacer vers</DropdownMenuLabel>
        {(Object.keys(statusLabels) as TaskStatus[]).map(status => <DropdownMenuItem key={status} disabled={status === task.status} onSelect={() => onStatus(status)}><CircleDashed />{statusLabels[status]}</DropdownMenuItem>)}
        <DropdownMenuSeparator /><DropdownMenuItem className="text-destructive focus:text-destructive" onSelect={onDelete}><Trash2 />Supprimer</DropdownMenuItem>
      </DropdownMenuContent></DropdownMenu>
    </div>
  </article>
}
export function StatusBadge({ status }: { status: TaskStatus }) {
  return <Badge variant="secondary" className={cn('gap-1.5 rounded-md border-0 px-2 py-1 text-[10px] font-semibold', status === 'TODO' ? 'bg-[#f1f3f3] text-[#62716f]' : status === 'IN_PROGRESS' ? 'bg-[#fff1dc] text-[#946418]' : 'bg-[#e5f5ef] text-[#217359]')}>
    {status === 'DONE' ? <Check className="size-3" /> : status === 'IN_PROGRESS' ? <ArrowUpRight className="size-3" /> : <Circle className="size-2 fill-current" />}{statusLabels[status]}
  </Badge>
}
