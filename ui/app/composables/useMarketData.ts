import type {
  TechnicalIndicators,
  TrendAnalysis,
  SupportResistance,
  IntradayQuote,
} from '~/types/marketdata'

export function useMarketData() {
  const { get, post } = useApi()

  const ticker = ref('')
  const indicators = ref<TechnicalIndicators | null>(null)
  const trend = ref<TrendAnalysis | null>(null)
  const trendHistory = ref<TrendAnalysis[]>([])
  const supportResistance = ref<SupportResistance | null>(null)
  const quote = ref<IntradayQuote | null>(null)

  const loading = ref(false)
  const analyzing = ref(false)
  const error = ref<string | null>(null)

  /** Load everything currently persisted/computable for a ticker. */
  async function loadAll(symbol: string) {
    const t = symbol.toUpperCase()
    ticker.value = t
    loading.value = true
    error.value = null
    // Latest trend may 404 if never analysed — swallow that individually.
    const [ind, sr, hist] = await Promise.allSettled([
      get<TechnicalIndicators>(`/market-data/${t}/indicators`, { days: '400' }),
      get<SupportResistance>(`/market-data/${t}/support-resistance`, { days: '400' }),
      get<TrendAnalysis[]>(`/market-data/${t}/trends`, { limit: '20' }),
    ])
    indicators.value = ind.status === 'fulfilled' ? ind.value : null
    supportResistance.value = sr.status === 'fulfilled' ? sr.value : null
    trendHistory.value = hist.status === 'fulfilled' ? hist.value : []
    trend.value = trendHistory.value[0] ?? null
    if (ind.status === 'rejected') {
      error.value = (ind.reason as any)?.message || 'Failed to load market data'
    }
    loading.value = false
  }

  /** Trigger a full LLM trend analysis (compute + verdict + persist), then refresh. */
  async function analyzeTrend(symbol: string) {
    const t = symbol.toUpperCase()
    ticker.value = t
    analyzing.value = true
    error.value = null
    try {
      trend.value = await post<TrendAnalysis>(`/market-data/${t}/analyze-trend?days=400`)
      await loadAll(t)
    } catch (e: any) {
      error.value = e?.message || 'Failed to analyze trend'
    } finally {
      analyzing.value = false
    }
  }

  /** Refresh the live intraday quote and persist it. */
  async function refreshQuote(symbol: string) {
    const t = symbol.toUpperCase()
    try {
      quote.value = await post<IntradayQuote>(`/market-data/${t}/quote/refresh`)
    } catch (e: any) {
      error.value = e?.message || 'Failed to refresh quote'
    }
  }

  return {
    ticker,
    indicators,
    trend,
    trendHistory,
    supportResistance,
    quote,
    loading,
    analyzing,
    error,
    loadAll,
    analyzeTrend,
    refreshQuote,
  }
}
