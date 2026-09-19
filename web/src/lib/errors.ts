import { ApiError } from './api'
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const detail = error.details as { detail?: string; errors?: Record<string, string> } | undefined
    if (detail?.detail) return detail.detail
    return 'La requête a échoué. Réessayez dans un instant.'
  }
  if (error instanceof TypeError) return 'Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.'
  return error instanceof Error ? error.message : 'Une erreur est survenue.'
}
export function errorCode(error: unknown) {
  return error instanceof ApiError ? (error.details as { code?: string })?.code : undefined
}
