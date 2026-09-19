import { useEffect, lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'
import { restoreSession } from '@/features/auth/session'
import { Brand } from '@/components/Brand'
import { LoaderCircle } from 'lucide-react'

const AuthPage = lazy(() => import('@/features/auth/AuthPage').then(module => ({ default: module.AuthPage })))
const Dashboard = lazy(() => import('@/features/tasks/Dashboard').then(module => ({ default: module.Dashboard })))

export default function App() {
  const { status, user } = useAppSelector(state => state.auth)
  useEffect(() => { void restoreSession() }, [])
  if (status === 'loading') return <div className="grid min-h-svh place-content-center gap-8"><Brand /><LoaderCircle aria-label="Restauration de session" className="mx-auto animate-spin text-brand" /></div>
  return <BrowserRouter><Suspense fallback={<div className="grid min-h-svh place-content-center"><LoaderCircle aria-label="Chargement de la page" className="animate-spin text-brand" /></div>}><Routes>
    <Route path="/" element={user ? <Dashboard key={user.id} /> : <Navigate to="/login" replace />} />
    {(['login', 'register', 'verify', 'forgot', 'reset'] as const).map(mode => <Route key={mode} path={'/' + mode} element={user ? <Navigate to="/" replace /> : <AuthPage key={mode} mode={mode} />} />)}
    <Route path="*" element={<Navigate to={user ? '/' : '/login'} replace />} />
  </Routes></Suspense></BrowserRouter>
}
