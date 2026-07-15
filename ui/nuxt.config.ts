export default defineNuxtConfig({
  compatibilityDate: '2025-05-01',

  future: {
    compatibilityVersion: 4,
  },

  modules: [
    '@nuxtjs/tailwindcss',
    '@nuxt/icon',
  ],

  devtools: { enabled: true },

  typescript: {
    strict: true,
    typeCheck: true,
  },

  runtimeConfig: {
    // ── Server-only (never exposed to the browser) ──
    // The real backend base URL + HTTP Basic credentials used by the
    // /api/v1/** proxy route. Override in prod via env:
    //   NUXT_API_PROXY_TARGET, NUXT_API_USERNAME, NUXT_API_PASSWORD
    apiProxyTarget: process.env.NUXT_API_PROXY_TARGET || 'http://localhost:4650',
    apiUsername: process.env.NUXT_API_USERNAME || 'admin',
    apiPassword: process.env.NUXT_API_PASSWORD || 'changeme',
    public: {
      // Empty → the browser calls this app same-origin (/api/v1/**) and the
      // server proxy forwards to the backend. Set NUXT_PUBLIC_API_BASE_URL only
      // if you want the browser to hit the backend directly (bypassing the proxy).
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || '',
    },
  },

  tailwindcss: {
    cssPath: '~/assets/css/main.css',
  },

  app: {
    head: {
      title: 'TradingAgents Dashboard',
      meta: [
        { name: 'description', content: 'AI-powered multi-agent trading analysis dashboard' },
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
      ],
    },
  },
})
