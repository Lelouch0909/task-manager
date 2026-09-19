import { routes } from '@vercel/config/v1'

// Vercel evaluates this configuration with the project's environment variables.
const target = process.env.API_PROXY_TARGET
if (!target) throw new Error('Set API_PROXY_TARGET to your backend origin in Vercel (https://api.example.com).')
const origin = new URL(target)
const backend = target.replace(/\/$/, '')
if (!['http:', 'https:'].includes(origin.protocol) || origin.username || origin.password ||
    origin.pathname !== '/' || origin.search || origin.hash) {
  throw new Error('API_PROXY_TARGET must be an HTTP(S) origin without /api, credentials or query parameters.')
}
if (process.env.VERCEL_ENV === 'production' && origin.protocol !== 'https:') {
  throw new Error('API_PROXY_TARGET must use HTTPS in production.')
}

export const config = {
  framework: 'vite',
  buildCommand: 'npm run build',
  outputDirectory: 'dist',
  rewrites: [
    routes.rewrite('/api/health', `${backend}/actuator/health/readiness`),
    routes.rewrite('/api/:path*', `${backend}/api/:path*`),
    routes.rewrite('/((?!api/|assets/).*)', '/index.html'),
  ],
  headers: [{ source: '/api/:path*', headers: [{ key: 'Cache-Control', value: 'private, no-store' }] }],
}
