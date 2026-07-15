/**
 * Server-side proxy for the trading-engine backend.
 *
 * The browser calls this Nuxt app same-origin at /api/v1/**; this handler (a
 * serverless function on Vercel) forwards the request to the real backend and
 * injects HTTP Basic credentials read from SERVER-ONLY runtime config. The
 * credentials therefore never reach the browser, and because the browser talks
 * to its own origin there is no CORS to configure.
 *
 * Scoped to /api/v1/** so it does not shadow other server routes (e.g.
 * @nuxt/icon's /api/_nuxt_icon endpoint).
 */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig()
  const target = String(config.apiProxyTarget || '').replace(/\/+$/, '')

  if (!target) {
    throw createError({ statusCode: 500, statusMessage: 'apiProxyTarget is not configured' })
  }

  const headers: Record<string, string> = {}
  if (config.apiUsername || config.apiPassword) {
    const token = Buffer
      .from(`${config.apiUsername}:${config.apiPassword}`)
      .toString('base64')
    headers.Authorization = `Basic ${token}`
  }

  // event.path is the full incoming path incl. query, e.g. /api/v1/portfolio?x=1
  return proxyRequest(event, `${target}${event.path}`, { headers })
})
