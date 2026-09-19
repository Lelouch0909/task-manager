import { Layers2 } from 'lucide-react'
import { cn } from '@/lib/utils'
export function Brand({ light = false, compact = false }: { light?: boolean; compact?: boolean }) {
  return <div className={cn('flex items-center gap-3', light && 'text-white')}>
    <span className={cn('relative grid size-10 place-items-center rounded-xl bg-brand text-white shadow-sm', light && 'bg-white/10 ring-1 ring-white/20')}><Layers2 className="size-5" /><span className="absolute -bottom-1 -right-1 size-3.5 rounded-full border-2 border-white bg-coral" /></span>
    {!compact && <div><span className="font-display text-lg font-extrabold tracking-tight">task<span className={light ? 'text-[#8ad9c6]' : 'text-brand'}>manager</span><span className="text-coral">.</span></span><p className={cn('text-[10px] font-medium uppercase tracking-[.21em] text-muted-foreground', light && 'text-white/50')}>Un peu plus clair, chaque jour</p></div>}
  </div>
}
