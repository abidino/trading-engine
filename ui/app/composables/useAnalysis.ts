import type { AnalysisRun, AnalysisRequestType } from '~/types'

export function useAnalysis() {
  const api = useApi()

  const runs = ref<AnalysisRun[]>([])
  const currentRun = ref<AnalysisRun | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  let pollInterval: ReturnType<typeof setInterval> | null = null

  async function suggestType(ticker: string): Promise<AnalysisRequestType> {
    try {
      const res = await api.get<{ requestType: AnalysisRequestType }>(`/analysis/suggest-type/${ticker}`)
      return res.requestType
    } catch {
      return 'DISCOVERY'
    }
  }

  async function startAnalysis(ticker: string, requestType?: AnalysisRequestType) {
    loading.value = true
    error.value = null
    try {
      const result = await api.post<{ runId: string }>('/analysis/run', { ticker, requestType })
      currentRun.value = {
        runId: result.runId,
        ticker,
        status: 'running',
        startedAt: new Date().toISOString(),
      }
      startPolling(result.runId)
    } catch (e: any) {
      error.value = e.message || 'Failed to start analysis'
    } finally {
      loading.value = false
    }
  }

  async function fetchStatus(runId: string) {
    try {
      currentRun.value = await api.get<AnalysisRun>(`/analysis/status/${runId}`)
      if (currentRun.value.status !== 'running') {
        stopPolling()
      }
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch analysis status'
    }
  }

  async function fetchRuns() {
    try {
      runs.value = await api.get<AnalysisRun[]>('/analysis/runs')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch analysis runs'
    }
  }

  async function fetchHistory(ticker: string) {
    try {
      return await api.get<AnalysisRun[]>(`/analysis/history/${ticker}`)
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch analysis history'
      return []
    }
  }

  function startPolling(runId: string) {
    stopPolling()
    pollInterval = setInterval(() => fetchStatus(runId), 3000)
  }

  function stopPolling() {
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
    }
  }

  onUnmounted(() => {
    stopPolling()
  })

  return {
    runs: readonly(runs),
    currentRun: readonly(currentRun),
    loading: readonly(loading),
    error: readonly(error),
    startAnalysis,
    suggestType,
    fetchStatus,
    fetchRuns,
    fetchHistory,
  }
}
