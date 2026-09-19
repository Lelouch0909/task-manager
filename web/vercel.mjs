// Vercel evaluates this configuration with the project's environment variables.
const target = process.env.API_PROXY_TARGET
if (!target) throw new Error('Set API_PROXY_TARGET to your backend origin in Vercel (https://api.example.com).')
const origin = new URL(target)
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
    { source: '/api/health', destination: `${origin.origin}/actuator/health/readiness` },
    { source: '/api/:path*', destination: `${origin.origin}/api/:path*` },
    { source: '/((?!api/|assets/).*)', destination: '/index.html' },
  ],
  headers: [{ source: '/api/:path*', headers: [{ key: 'Cache-Control', value: 'private, no-store' }] }],
}
