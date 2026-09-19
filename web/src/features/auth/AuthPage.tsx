import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, ArrowUpRight, Check, Eye, EyeOff, LoaderCircle, ShieldCheck, Sparkles, AlertCircle } from 'lucide-react'
import { toast } from 'sonner'
import { Brand } from '@/components/Brand'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiFetch } from '@/lib/api'
import { errorMessage, errorCode } from '@/lib/errors'
import { login } from './session'
import { useAppSelector } from '@/app/hooks'

type Mode = 'login' | 'register' | 'verify' | 'forgot' | 'reset'
const copy: Record<Mode, { eyebrow: string; title: string; subtitle: string; action: string }> = {
  login: { eyebrow: 'À VOUS DE JOUER', title: 'On reprend le fil ?', subtitle: 'Vos projets vous attendent. Faites une place à la prochaine petite victoire.', action: 'Entrer dans mon espace' },
  register: { eyebrow: 'UN NOUVEAU DÉPART', title: 'Faites de la place à vos idées.', subtitle: 'Créez votre compte et avancez, une tâche à la fois.', action: 'Créer mon compte' },
  verify: { eyebrow: 'PRESQUE ARRIVÉ', title: 'Un dernier petit geste.', subtitle: 'Saisissez le code à six chiffres reçu par email pour activer votre compte.', action: 'Vérifier mon email' },
  forgot: { eyebrow: 'ON S’OCCUPE DE VOUS', title: 'Un nouveau départ.', subtitle: 'Nous vous enverrons un code pour choisir un nouveau mot de passe.', action: 'Recevoir mon code' },
  reset: { eyebrow: 'EN TOUTE SÉCURITÉ', title: 'À vous de choisir.', subtitle: 'Saisissez votre code de récupération et votre nouveau mot de passe.', action: 'Enregistrer le mot de passe' },
}
export function AuthPage({ mode }: { mode: Mode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState((location.state as { email?: string } | null)?.email ?? '')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [code, setCode] = useState('')
  const [visible, setVisible] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [recovery, setRecovery] = useState(false)
  const [cooldown, setCooldown] = useState(0)
  const startupError = useAppSelector(state => state.auth.error)
  const text = copy[mode]
  useEffect(() => {
    if (!cooldown) return
    const timer = setTimeout(() => setCooldown(value => value - 1), 1000)
    return () => clearTimeout(timer)
  }, [cooldown])
  async function post(path: string, body: unknown) {
    return apiFetch('/auth/' + path, { method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  }
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setRecovery(false)
    if (['register', 'reset'].includes(mode) && new TextEncoder().encode(password).length > 72) {
      setError('Votre mot de passe est trop long (72 octets maximum).'); return
    }
    setBusy(true)
    const address = email.trim()
    try {
      if (mode === 'login') { await login(address, password); navigate('/', { replace: true }) }
      if (mode === 'register') {
        await post('register', { displayName: name.trim(), email: address, password })
        toast.success('Votre compte est créé. Vérifiez votre email.')
        navigate('/verify', { state: { email: address } })
      }
      if (mode === 'verify') {
        await post('email/verify', { email: address, code })
        toast.success('Email vérifié. Bienvenue !')
        navigate('/login', { state: { email: address } })
      }
      if (mode === 'forgot') {
        await post('password/forgot', { email: address })
        toast.success('Si un compte correspond à cette adresse, un code a été envoyé.')
        navigate('/reset', { state: { email: address } })
      }
      if (mode === 'reset') {
        await post('password/reset', { email: address, code, password })
        toast.success('Mot de passe modifié. Vous pouvez vous connecter.')
        navigate('/login', { state: { email: address } })
      }
    } catch (e) {
      setError(errorMessage(e))
      if (errorCode(e) === 'email_not_verified' || (mode === 'register' && errorCode(e) === 'email_unavailable')) setRecovery(true)
    } finally { setBusy(false) }
  }
  async function resend() {
    setBusy(true); setError('')
    try {
      await post(mode === 'reset' ? 'password/forgot' : 'email/resend', { email: email.trim() })
      setCooldown(60); toast.success('Demande de renvoi prise en compte.')
    } catch (e) { setError(errorMessage(e)); setCooldown(60) }
    finally { setBusy(false) }
  }
  return <main className="auth-scene">
    <header className="auth-header">
      <Brand />
      <div className="flex flex-wrap items-center gap-y-1 text-xs text-muted-foreground sm:text-sm">
        {mode === 'login' ? <>Pas encore de compte ? <Link className="ml-2 font-semibold text-primary hover:underline" to="/register">Rejoignez-nous <span aria-hidden="true">↗</span></Link></> :
          <>Déjà un compte ? <Link className="ml-2 font-semibold text-primary hover:underline" to="/login">Se connecter</Link></>}
      </div>
    </header>
    <div className="auth-layout">
      <section className="auth-story" aria-label="Un peu d’organisation, beaucoup de possibilités">
        <div className="flex items-center gap-3 text-xs font-semibold uppercase tracking-[.18em]"><span className="h-px w-8 bg-current" /> De l’idée au premier pas</div>
        <h2 className="auth-headline">Moins de<br /><span className="auth-outline">plus tard.</span><br />Plus de <span className="auth-now">c’est fait.<svg viewBox="0 0 300 18" preserveAspectRatio="none" aria-hidden="true"><path d="M4 12 Q150 -2 294 9 M32 17 Q180 7 265 15" /></svg></span></h2>
        <p className="mt-7 max-w-sm text-sm leading-7 text-muted-foreground">Les grandes idées commencent par une petite tâche.<br className="hidden sm:block" /> Donnez-leur un point de départ.</p>
        <div className="auth-art" aria-hidden="true">
          <div className="auth-orbit" />
          <div className="auth-spark"><Sparkles strokeWidth={1.3} /></div>
          <div className="auth-ticket"><span className="text-[10px] font-bold tracking-[.18em]">LE PROGRAMME DU JOUR</span><div className="mt-5 flex items-center gap-3"><span className="grid size-7 place-items-center rounded-full bg-brand text-white"><Check className="size-4" /></span><span className="text-sm text-muted-foreground line-through">Remettre à demain</span></div><div className="mt-4 flex items-center gap-3"><span className="size-7 rounded-full border-2 border-coral" /><span className="font-display text-lg font-bold">Se lancer, enfin.</span></div><div className="mt-5 flex items-center justify-between border-t border-dashed pt-4 text-xs"><span>Une chose à la fois.</span><ArrowUpRight className="size-5 text-primary" /></div></div>
          <div className="auth-stamp"><ArrowUpRight className="size-8" /><span>PLACE<br />À L’ACTION</span></div>
        </div>
      </section>
      <section className="auth-form-panel">
      <div className="auth-panel-tab" aria-hidden="true"><span className="size-2 rounded-full bg-coral" /> VOTRE PROCHAIN CHAPITRE</div>
      <div className="page-enter mx-auto w-full max-w-[400px]">
        {mode !== 'login' && mode !== 'register' && <Link to="/login" className="mb-8 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-primary"><ArrowLeft className="size-4" /> Retour à la connexion</Link>}
        <div className="mb-7 flex items-center justify-between"><span className="grid size-12 place-items-center rounded-2xl bg-coral/15 text-[#a53d2b]"><ArrowUpRight className="size-6" /></span><span className="font-mono text-xs text-muted-foreground">{mode === 'login' ? 'BONJOUR / HELLO' : 'UN PAS DE PLUS'}</span></div>
        <p className="muted-label mb-3 text-primary">{text.eyebrow}</p>
        <h1 className="text-3xl font-bold leading-tight tracking-tight sm:text-4xl">{text.title}</h1>
        <p className="mb-8 mt-4 text-sm leading-6 text-muted-foreground">{text.subtitle}</p>
        <form onSubmit={submit} className="space-y-5">
          {mode === 'register' && <div className="field"><Label htmlFor="name">Votre nom</Label><Input id="name" className="auth-input" autoComplete="name" placeholder="Comment vous appeler ?" required maxLength={100} value={name} onChange={e => setName(e.target.value)} /></div>}
          <div className="field"><Label htmlFor="email">Adresse email</Label><Input id="email" className="auth-input" type="email" autoComplete="email" placeholder="vous@exemple.com" required maxLength={254} value={email} onChange={e => setEmail(e.target.value)} /></div>
          {['verify', 'reset'].includes(mode) && <div className="field"><Label htmlFor="code">Code à six chiffres</Label><Input id="code" className="auth-input text-center font-mono text-2xl tracking-[.4em]" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" required value={code} onChange={e => setCode(e.target.value.replace(/\D/g, ''))} /><p className="text-xs text-muted-foreground">Le code est valable 10 minutes. Pensez aux courriers indésirables.</p></div>}
          {['login', 'register', 'reset'].includes(mode) && <div className="field">
            <div className="flex items-center justify-between"><Label htmlFor="password">{mode === 'reset' ? 'Nouveau mot de passe' : 'Mot de passe'}</Label>{mode === 'login' && <Link to="/forgot" className="text-xs font-medium text-primary hover:underline">Mot de passe oublié ?</Link>}</div>
            <div className="relative"><Input id="password" className="auth-input pr-12" type={visible ? 'text' : 'password'} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} placeholder="Votre mot de passe" required minLength={mode === 'login' ? 1 : 8} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} /><button type="button" aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'} onClick={() => setVisible(!visible)} className="absolute right-4 top-4 text-muted-foreground">{visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</button></div>
            {mode !== 'login' && <p className="text-xs text-muted-foreground">Au moins 8 caractères pour protéger votre espace.</p>}
          </div>}
          {(error || (mode === 'login' && startupError)) && <div role="alert" className="flex items-start gap-2 rounded-xl bg-red-50 p-3 text-sm text-destructive"><AlertCircle className="mt-0.5 size-4 shrink-0" /><span>{error || startupError}{recovery && <Link className="mt-2 block underline" to="/verify" state={{ email }}>Vérifier mon email ou renvoyer le code</Link>}</span></div>}
          <Button type="submit" disabled={busy} className="auth-submit group h-13 w-full rounded-xl text-sm">{busy ? <><LoaderCircle className="animate-spin" /> Un petit instant…</> : <>{text.action}<ArrowRight className="ml-auto size-4 transition-transform group-hover:translate-x-1" /></>}</Button>
          {['verify', 'reset'].includes(mode) && <Button type="button" variant="ghost" className="w-full" disabled={busy || cooldown > 0 || !email} onClick={() => void resend()}>{cooldown ? `Renvoyer dans ${cooldown}s` : 'Je n’ai pas reçu de code'}</Button>}
        </form>
        <div className="mt-8 flex items-center justify-center gap-2 text-xs text-muted-foreground"><ShieldCheck className="size-4 text-brand" /> Votre espace est personnel et sécurisé.</div>
      </div>
      </section>
    </div>
    <footer className="auth-footer"><span>Un peu d’organisation. Beaucoup de possibilités.</span><span className="flex items-center gap-2"><span className="size-2 rounded-full bg-brand" /><span className="size-2 rounded-full bg-coral" /> À votre rythme.</span></footer>
  </main>
}
