import type { NewsRecord, SocialSignal } from '~/types/intelligence'

export function useIntelligence() {
  const { get, post } = useApi()

  const news = ref<NewsRecord[]>([])
  const social = ref<SocialSignal[]>([])
  const ticker = ref('')
  const activeTab = ref<'news' | 'social'>('news')
  const loading = ref(false)
  const scanning = ref(false)
  const error = ref<string | null>(null)
  const scanMessage = ref<string | null>(null)

  async function fetchNews(tickerSymbol: string, days = 7) {
    loading.value = true
    error.value = null
    try {
      news.value = await get<NewsRecord[]>(
        `/intelligence/${tickerSymbol.toUpperCase()}/news`,
        { days: String(days) },
      )
    } catch (e: any) {
      error.value = e?.message || 'Failed to fetch news'
    } finally {
      loading.value = false
    }
  }

  async function fetchSocial(tickerSymbol: string) {
    loading.value = true
    error.value = null
    try {
      social.value = await get<SocialSignal[]>(`/intelligence/${tickerSymbol.toUpperCase()}/social`)
    } catch (e: any) {
      error.value = e?.message || 'Failed to fetch social signals'
    } finally {
      loading.value = false
    }
  }

  async function fetchAll(tickerSymbol: string) {
    ticker.value = tickerSymbol.toUpperCase()
    await Promise.all([fetchNews(tickerSymbol), fetchSocial(tickerSymbol)])
  }

  /** Trigger a fresh scan + LLM classification for the ticker, then reload. */
  async function scanNews(tickerSymbol: string, limit = 20) {
    const t = tickerSymbol.toUpperCase()
    scanning.value = true
    error.value = null
    scanMessage.value = null
    try {
      const res = await post<{ newArticles: number }>(`/intelligence/news/scan/${t}?limit=${limit}`)
      scanMessage.value = `${res.newArticles} new article(s) scanned for ${t}`
      ticker.value = t
      await fetchNews(t)
    } catch (e: any) {
      error.value = e?.message || 'Failed to scan news'
    } finally {
      scanning.value = false
    }
  }

  /** Trigger a macro (market-wide) news scan; refreshes current ticker view. */
  async function scanMacro(limit = 30) {
    scanning.value = true
    error.value = null
    scanMessage.value = null
    try {
      const res = await post<{ newArticles: number }>(`/intelligence/news/scan-macro?limit=${limit}`)
      scanMessage.value = `${res.newArticles} macro article(s) scanned`
      if (ticker.value) await fetchNews(ticker.value)
    } catch (e: any) {
      error.value = e?.message || 'Failed to scan macro news'
    } finally {
      scanning.value = false
    }
  }

  return {
    news,
    social,
    ticker,
    activeTab,
    loading,
    scanning,
    error,
    scanMessage,
    fetchNews,
    fetchSocial,
    fetchAll,
    scanNews,
    scanMacro,
  }
}
