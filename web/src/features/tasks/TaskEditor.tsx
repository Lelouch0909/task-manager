import { useState, type FormEvent } from 'react'
import { LoaderCircle, ArrowRight } from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { authRequest } from '@/features/auth/session'
import { statusLabels, taskSchema, type Task, type TaskStatus } from '@/lib/contracts'
import { errorMessage } from '@/lib/errors'

export function TaskEditor({ task, onClose, onSaved }: { task?: Task; onClose: () => void; onSaved: () => void }) {
  const [title, setTitle] = useState(task?.title ?? '')
  const [description, setDescription] = useState(task?.description ?? '')
  const [status, setStatus] = useState<TaskStatus>(task?.status ?? 'TODO')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function save(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) { setError('Donnez un titre à votre tâche.'); return }
    setBusy(true); setError('')
    try {
      taskSchema.parse(await authRequest(task ? '/tasks/' + task.id : '/tasks', {
        method: task ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: title.trim(), description: description || null, status }),
      }))
      toast.success(task ? 'Les changements sont enregistrés.' : 'Une idée de plus prête à avancer.')
      onSaved(); onClose()
    } catch (e) { setError(errorMessage(e)) }
    finally { setBusy(false) }
  }
  return <Dialog open onOpenChange={open => { if (!open && !busy) onClose() }}>
    <DialogContent className="rounded-2xl p-7 sm:max-w-lg" onInteractOutside={e => { if (busy) e.preventDefault() }}>
      <DialogHeader><p className="muted-label mb-2 text-primary">{task ? 'UN PETIT AJUSTEMENT' : 'FAITES LE PREMIER PAS'}</p><DialogTitle className="text-2xl">{task ? 'Modifier la tâche' : 'Une nouvelle tâche'}</DialogTitle><DialogDescription>Une intention claire, c’est déjà un pas en avant.</DialogDescription></DialogHeader>
      <form onSubmit={save} className="mt-3 space-y-5">
        <div className="field"><Label htmlFor="task-title">Titre de la tâche</Label><Input autoFocus id="task-title" placeholder="Qu’aimeriez-vous accomplir ?" maxLength={200} required value={title} onChange={e => setTitle(e.target.value)} className="h-11" /></div>
        <div className="field"><div className="flex justify-between"><Label htmlFor="task-description">Quelques détails</Label><span className="text-xs text-muted-foreground">Facultatif</span></div><Textarea id="task-description" placeholder="Les idées, les étapes, les petits détails à garder…" maxLength={5000} rows={4} value={description} onChange={e => setDescription(e.target.value)} /><span className="text-right text-[11px] text-muted-foreground">{description.length} / 5 000</span></div>
        <div className="field"><Label htmlFor="task-status">Statut</Label><Select value={status} onValueChange={value => setStatus(value as TaskStatus)}><SelectTrigger id="task-status" className="w-full"><SelectValue /></SelectTrigger><SelectContent>{Object.entries(statusLabels).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select></div>
        {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-destructive">{error}</p>}
        <DialogFooter className="mt-7"><Button type="button" variant="ghost" disabled={busy} onClick={onClose}>Annuler</Button><Button type="submit" disabled={busy}>{busy ? <LoaderCircle className="animate-spin" /> : <ArrowRight />}{task ? 'Enregistrer' : 'Créer la tâche'}</Button></DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
}
