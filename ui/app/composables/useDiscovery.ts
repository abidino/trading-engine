import type { DiscoveredStock, DiscoveryFilter } from '~/types'
export function useDiscovery() {
  const api = useApi()

  const stocks = ref<DiscoveredStock[]>([])
  const filters = ref<DiscoveryFilter[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchDiscoveredStocks() {
    loading.value = true
    error.value = null
    try {
      stocks.value = await api.get<DiscoveredStock[]>('/discovery/stocks')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch discovered stocks'
    } finally {
      loading.value = false
    }
  }

  async function fetchFilters() {
    try {
      filters.value = await api.get<DiscoveryFilter[]>('/discovery/filters')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch filters'
    }
  }

  async function createFilter(body: { name: string, description?: string, selections: Record<string, string>, rawFinvizFilters?: string }) {
    try {
      const created = await api.post<DiscoveryFilter>('/discovery/filters', body)
      filters.value = [...filters.value, created]
      return created
    } catch (e: any) {
      error.value = e.message || 'Failed to create filter'
      throw e
    }
  }

  async function toggleFilter(filterId: string, activate: boolean) {
    const action = activate ? 'activate' : 'deactivate'
    try {
      await api.post(`/discovery/filters/${filterId}/${action}`)
      filters.value = filters.value.map(f =>
        f.id === filterId ? { ...f, active: activate } : f,
      )
    } catch (e: any) {
      error.value = e.message || `Failed to ${action} filter`
    }
  }

  async function deleteFilter(filterId: string) {
    try {
      await api.del(`/discovery/filters/${filterId}`)
      filters.value = filters.value.filter(f => f.id !== filterId)
    } catch (e: any) {
      error.value = e.message || 'Failed to delete filter'
    }
  }

  async function runDiscovery() {
    loading.value = true
    error.value = null
    try {
      const result = await api.post<{ newStocksFound: number }>('/discovery/run')
      await fetchDiscoveredStocks()
      return result.newStocksFound
    } catch (e: any) {
      error.value = e.message || 'Failed to run discovery'
      return 0
    } finally {
      loading.value = false
    }
  }

  async function runAdHoc(criteria: { selections: Record<string, string>, rawFinvizFilters?: string }) {
    loading.value = true
    error.value = null
    try {
      const results = await api.post<DiscoveredStock[]>('/discovery/run/ad-hoc', criteria)
      stocks.value = results
      return results.length
    } catch (e: any) {
      error.value = e.message || 'Failed to run ad-hoc discovery'
      return 0
    } finally {
      loading.value = false
    }
  }

  async function promoteStock(ticker: string) {
    try {
      await api.post(`/discovery/stocks/${ticker}/promote`)
      stocks.value = stocks.value.filter(s => s.ticker !== ticker)
    } catch (e: any) {
      error.value = e.message || 'Failed to promote stock'
      throw e
    }
  }

  async function dismissStock(ticker: string) {
    try {
      await api.post(`/discovery/stocks/${ticker}/dismiss`)
      stocks.value = stocks.value.filter(s => s.ticker !== ticker)
    } catch (e: any) {
      error.value = e.message || 'Failed to dismiss stock'
      throw e
    }
  }

  return {
    stocks: readonly(stocks),
    filters: readonly(filters),
    loading: readonly(loading),
    error: readonly(error),
    fetchDiscoveredStocks,
    fetchFilters,
    createFilter,
    toggleFilter,
    deleteFilter,
    runDiscovery,
    runAdHoc,
    promoteStock,
    dismissStock,
  }
}
