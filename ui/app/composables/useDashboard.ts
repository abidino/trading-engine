import type { PortfolioSummary, SectorBreakdown, PerformanceEntry } from '~/types'

export function useDashboard() {
  const api = useApi()

  const summary = ref<PortfolioSummary | null>(null)
  const sectors = ref<SectorBreakdown[]>([])
  const performance = ref<PerformanceEntry[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSummary() {
    loading.value = true
    error.value = null
    try {
      summary.value = await api.get<PortfolioSummary>('/dashboard/summary')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch dashboard summary'
    } finally {
      loading.value = false
    }
  }

  async function fetchSectors() {
    try {
      sectors.value = await api.get<SectorBreakdown[]>('/dashboard/sectors')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch sectors'
    }
  }

  async function fetchPerformance() {
    try {
      performance.value = await api.get<PerformanceEntry[]>('/dashboard/performance')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch performance'
    }
  }

  async function runAutoPromote() {
    try {
      await api.post('/dashboard/auto-promote')
    } catch (e: any) {
      error.value = e.message || 'Failed to run auto-promote'
    }
  }

  async function runScheduledAnalysis() {
    try {
      await api.post('/dashboard/run-analysis')
    } catch (e: any) {
      error.value = e.message || 'Failed to run scheduled analysis'
    }
  }

  // Best-effort: refresh live quotes for held tickers before showing valuations.
  async function refreshQuotes() {
    try {
      await api.post('/portfolio/refresh')
    } catch {
      // Non-fatal — dashboard falls back to last known prices.
    }
  }

  return {
    summary: readonly(summary),
    sectors: readonly(sectors),
    performance: readonly(performance),
    loading: readonly(loading),
    error: readonly(error),
    fetchSummary,
    fetchSectors,
    fetchPerformance,
    refreshQuotes,
    runAutoPromote,
    runScheduledAnalysis,
  }
}
